package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** 如 http://127.0.0.1:9000 */
    private String endpoint;
    /** 浏览器和 AI 服务均可访问的 MinIO 公网 HTTPS 地址。 */
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    /** 预签名 URL 有效期（秒），默认 12 小时 */
    private long signedUrlExpireSeconds = 43200L;
}
