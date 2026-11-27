package com.agent.biogenie.module.account.controller;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.Result;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.file.FileExtractResultDTO;
import com.agent.biogenie.utils.LogUtils;
import com.agent.biogenie.utils.MinioUtil;
import com.agent.biogenie.utils.TikaTextExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 文件管理控制器
 */
@Slf4j
@Tag(name = "文件管理", description = "MinIO文件上传、删除、文档文本抽取等接口")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final MinioUtil minioUtil;
    private final TikaTextExtractor tikaTextExtractor;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>();

    /** 文本抽取允许的文档类型（不含图片） */
    private static final Set<String> ALLOWED_EXTRACT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/markdown",
            "text/x-markdown"
    );

    static {
        ALLOWED_FILE_TYPES.addAll(ALLOWED_IMAGE_TYPES);
        ALLOWED_FILE_TYPES.addAll(Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain"
        ));
    }

    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024;
    /** 文本抽取单文件上限 10MB */
    private static final long MAX_EXTRACT_SIZE = 10 * 1024 * 1024;

    @Operation(summary = "上传图片")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(
            HttpServletRequest request,
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "存储路径前缀") @RequestParam(defaultValue = "images/") String prefix) {
        validateImage(file);
        return doUpload(resolveUserId(request), "UPLOAD_IMAGE", "FILE_UPLOAD_IMAGE", file, prefix);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<String> uploadFile(
            HttpServletRequest request,
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "存储路径前缀") @RequestParam(defaultValue = "files/") String prefix) {
        validateFile(file);
        return doUpload(resolveUserId(request), "UPLOAD_FILE", "FILE_UPLOAD", file, prefix);
    }

    @Operation(summary = "抽取文档文本",
            description = "支持 pdf/doc/docx/xls/xlsx/txt/md，单文件≤10MB。返回纯文本，超长截断。")
    @PostMapping("/extract")
    public Result<FileExtractResultDTO> extractText(
            HttpServletRequest request,
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file) {
        String userId = resolveUserId(request);
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("FILE_EXTRACT");
        String fileName = file.getOriginalFilename();
        try {
            validateExtractFile(file);
            FileExtractResultDTO result = tikaTextExtractor.extract(file);
            LogUtils.logFileOperation("EXTRACT", fileName, "SUCCESS");
            return Result.ok("提取成功", result);
        } catch (Exception e) {
            LogUtils.logFileOperation("EXTRACT", fileName, "FAILED");
            throw e;
        } finally {
            monitor.end("file=" + fileName);
        }
    }

    @Operation(summary = "批量上传图片")
    @PostMapping("/upload/images")
    public Result<List<String>> uploadImages(
            HttpServletRequest request,
            @Parameter(description = "图片文件列表") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "存储路径前缀") @RequestParam(defaultValue = "images/") String prefix) {
        String userId = resolveUserId(request);
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("FILE_UPLOAD_IMAGES");
        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                validateImage(file);
                String permanentUrl = minioUtil.upload(file, prefix);
                urls.add(minioUtil.toAccessUrl(permanentUrl));
                LogUtils.logFileOperation("UPLOAD_IMAGE", file.getOriginalFilename(), "SUCCESS");
            }
            return Result.ok("上传成功", urls);
        } catch (Exception e) {
            LogUtils.logFileOperation("UPLOAD_IMAGES", "batch", "FAILED");
            throw e;
        } finally {
            monitor.end("count=" + urls.size());
        }
    }

    @Operation(summary = "换签文件访问 URL",
            description = "私有桶裸链或过期签名 URL 换取新的预签名 GET 地址（默认 12h）。")
    @GetMapping("/signed-url")
    public Result<String> signedUrl(
            @Parameter(description = "文件裸链、objectKey 或过期签名 URL") @RequestParam String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "fileUrl 不能为空");
        }
        // 必须用 (message, data)：Result.ok(String) 会把 URL 写到 message 而 data 为空
        return Result.ok("换签成功", minioUtil.toAccessUrl(fileUrl));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result<String> deleteFile(
            HttpServletRequest request,
            @Parameter(description = "文件完整URL") @RequestParam String fileUrl) {
        String userId = resolveUserId(request);
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("FILE_DELETE");
        try {
            String objectKey = minioUtil.extractObjectKey(fileUrl);
            minioUtil.delete(objectKey);
            LogUtils.logFileOperation("DELETE", objectKey, "SUCCESS");
            return Result.ok("删除成功");
        } catch (Exception e) {
            LogUtils.logFileOperation("DELETE", fileUrl, "FAILED");
            throw e;
        } finally {
            monitor.end("fileUrl=" + fileUrl);
        }
    }

    private Result<String> doUpload(String userId, String operation, String perfOp,
                                    MultipartFile file, String prefix) {
        String fileName = file.getOriginalFilename();
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor(perfOp);
        try {
            String permanentUrl = minioUtil.upload(file, prefix);
            String accessUrl = minioUtil.toAccessUrl(permanentUrl);
            LogUtils.logFileOperation(operation, fileName, "SUCCESS");
            return Result.ok("上传成功", accessUrl);
        } catch (Exception e) {
            LogUtils.logFileOperation(operation, fileName, "FAILED");
            throw e;
        } finally {
            monitor.end("file=" + fileName);
        }
    }

    private void validateImage(MultipartFile file) {
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_SUPPORTED, "只支持 jpg/png/gif/webp/bmp 格式");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED, "图片大小不能超过20MB");
        }
    }

    private void validateFile(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED, "文件大小不能超过200MB");
        }
    }

    /** 浏览器可能附带 charset（例如 text/plain;charset=utf-8），白名单仅比较基础 MIME。 */
    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameterStart = contentType.indexOf(';');
        String baseType = parameterStart >= 0
                ? contentType.substring(0, parameterStart)
                : contentType;
        return baseType.trim().toLowerCase(Locale.ROOT);
    }

    private void validateExtractFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean typeOk = contentType != null && ALLOWED_EXTRACT_TYPES.contains(contentType);
        boolean extOk = name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")
                || name.endsWith(".xls") || name.endsWith(".xlsx")
                || name.endsWith(".txt") || name.endsWith(".md");
        if (!typeOk && !extOk) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_SUPPORTED, "请上传文档（pdf/doc/docx/xls/xlsx/txt/md）");
        }
        if (file.getSize() > MAX_EXTRACT_SIZE) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED, "抽取文件大小不能超过10MB");
        }
    }

    private String resolveUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? String.valueOf(userId) : "anonymous";
    }
}
