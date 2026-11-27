package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云号码认证（Dypnsapi）短信验证码配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String accessKeyId;

    private String accessKeySecret;

    /** 短信签名名称 */
    private String signName;

    /** 短信模板 CODE */
    private String templateCode;

    /** 方案名称，需与发送/核验保持一致 */
    private String schemeName;

    /**
     * 模板参数 JSON。使用 ##code## 由阿里云生成验证码，才能用 CheckSmsVerifyCode 核验。
     * 示例：{"code":"##code##","min":"5"}
     */
    private String templateParam = "{\"code\":\"##code##\",\"min\":\"5\"}";

    /** 验证码长度 4~8，默认 6 */
    private Long codeLength = 6L;

    private String region = "cn-hangzhou";

    private String endpoint = "dypnsapi.aliyuncs.com";

    /** 本地发送间隔限流（秒） */
    private long rateLimitSeconds = 60;
}
