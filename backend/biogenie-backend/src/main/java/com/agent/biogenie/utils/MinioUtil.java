package com.agent.biogenie.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类（私有桶：对外用预签名 URL，库内存裸链）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioProperties minioProperties;

    private MinioClient minioClient;
    private MinioClient signingClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        this.signingClient = MinioClient.builder()
                .endpoint(accessEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        ensureBucket();
        log.info("MinIO客户端初始化完成, endpoint={}, bucket={}",
                minioProperties.getEndpoint(), minioProperties.getBucketName());
    }

    private void ensureBucket() {
        String bucket = minioProperties.getBucketName();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已创建: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("检查/创建 MinIO bucket 失败(忽略，后续上传可能失败): bucket={}, err={}",
                    bucket, e.getMessage());
        }
    }

    /**
     * 上传文件
     *
     * @param file   文件
     * @param prefix 存储路径前缀 (如 "avatar/", "article/")
     * @return 持久化裸链（无签名），便于落库；对外访问请再用 {@link #toAccessUrl(String)}
     */
    public String upload(MultipartFile file, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = (prefix != null ? prefix : "") + IdUtil.simpleUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            byte[] bytes = IoUtil.readBytes(inputStream);
            String contentType = resolveContentType(file.getContentType(), objectKey);
            if (StrUtil.isBlank(contentType)) {
                contentType = "application/octet-stream";
            }
            String fileName = StrUtil.blankToDefault(originalFilename, objectKey);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Disposition",
                    "inline;filename=\"" + sanitizeFileName(fileName) + "\"");

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .headers(headers)
                    .build());
            log.info("文件上传成功: {} -> {} (size: {} bytes)",
                    originalFilename, objectKey, bytes.length);
        } catch (IOException e) {
            log.error("文件上传失败: {}", originalFilename, e);
            throw new RuntimeException("文件上传失败", e);
        } catch (Exception e) {
            log.error("文件上传失败: {}", originalFilename, e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }

        return buildPermanentUrl(objectKey);
    }

    /**
     * 删除文件
     *
     * @param objectKey 文件在 MinIO 中的 key（或完整 URL）
     */
    public void delete(String objectKey) {
        String key = extractObjectKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(key)
                    .build());
            log.info("文件删除成功: {}", key);
        } catch (Exception e) {
            log.error("文件删除失败: {}", key, e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "文件删除失败");
        }
    }

    /**
     * 生成预签名 GET URL。预览依赖对象自身的 Content-Type / Content-Disposition。
     */
    public String generateSignedUrl(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new BizException(ResultCode.BAD_REQUEST, "objectKey 不能为空");
        }
        String key = extractObjectKey(objectKey);
        long expireSeconds = minioProperties.getSignedUrlExpireSeconds() > 0
                ? minioProperties.getSignedUrlExpireSeconds()
                : 43200L;
        try {
            return (signingClient != null ? signingClient : minioClient).getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucketName())
                    .object(key)
                    .expiry((int) Math.min(expireSeconds, Integer.MAX_VALUE), TimeUnit.SECONDS)
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成 MinIO 预签名 URL 失败: key={}", key, e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "生成文件访问链接失败");
        }
    }

    /**
     * 将库内裸链 / objectKey / 已过期签名 URL 转为可访问的预签名 URL。
     * 非本 Bucket 的外链原样返回。
     */
    public String toAccessUrl(String storedUrlOrKey) {
        if (StrUtil.isBlank(storedUrlOrKey)) {
            return storedUrlOrKey;
        }
        String trimmed = storedUrlOrKey.trim();
        if (!belongsToBucket(trimmed) && looksLikeHttpUrl(trimmed)) {
            return stripQuery(trimmed);
        }
        return generateSignedUrl(trimmed);
    }

    /**
     * 落库用：去掉签名 query，还原为稳定裸链；非本桶外链仅去 query。
     */
    public String toPermanentUrl(String urlOrKey) {
        if (StrUtil.isBlank(urlOrKey)) {
            return urlOrKey;
        }
        String trimmed = urlOrKey.trim();
        if (!belongsToBucket(trimmed) && looksLikeHttpUrl(trimmed)) {
            return stripQuery(trimmed);
        }
        String key = extractObjectKey(trimmed);
        return buildPermanentUrl(key);
    }

    /**
     * 从完整 URL（可含签名参数）或裸链中提取 objectKey。
     * path-style: {endpoint}/{bucket}/{objectKey}
     */
    public String extractObjectKey(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        String withoutQuery = stripQuery(url.trim());
        for (String prefix : new String[]{permanentUrlPrefix(), publicUrlPrefix()}) {
            if (withoutQuery.startsWith(prefix)) {
                return withoutQuery.substring(prefix.length());
            }
        }
        // 兼容仅 objectKey
        return withoutQuery.startsWith("/") ? withoutQuery.substring(1) : withoutQuery;
    }

    public String buildPermanentUrl(String objectKey) {
        return publicUrlPrefix() + objectKey;
    }

    boolean belongsToBucket(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        String withoutQuery = stripQuery(url.trim());
        return withoutQuery.startsWith(permanentUrlPrefix()) || withoutQuery.startsWith(publicUrlPrefix());
    }

    private String permanentUrlPrefix() {
        String endpoint = normalizeEndpoint(minioProperties.getEndpoint());
        return endpoint + "/" + minioProperties.getBucketName() + "/";
    }

    private String publicUrlPrefix() {
        return normalizeEndpoint(accessEndpoint()) + "/" + minioProperties.getBucketName() + "/";
    }

    private String accessEndpoint() {
        return StrUtil.isBlank(minioProperties.getPublicEndpoint())
                ? minioProperties.getEndpoint() : minioProperties.getPublicEndpoint();
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean looksLikeHttpUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    static String resolveContentType(String multipartContentType, String objectKey) {
        String guessed = guessContentType(objectKey);
        if (StrUtil.isBlank(multipartContentType)
                || "application/octet-stream".equalsIgnoreCase(multipartContentType)) {
            return guessed;
        }
        if (StrUtil.isNotBlank(guessed)) {
            return guessed;
        }
        return multipartContentType;
    }

    static String sanitizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "file";
        }
        return fileName.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    static String guessContentType(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            return null;
        }
        String key = objectKey.toLowerCase(Locale.ROOT);
        int slash = key.lastIndexOf('/');
        String name = slash >= 0 ? key.substring(slash + 1) : key;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot + 1);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            default -> null;
        };
    }

    static String stripQuery(String url) {
        if (url == null) {
            return null;
        }
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }

    /** 单测注入用 */
    void setMinioClient(MinioClient minioClient) {
        this.minioClient = minioClient;
        this.signingClient = minioClient;
    }
}
