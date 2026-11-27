package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对话分享链接配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "share")
public class ShareProperties {

    /**
     * 前端站点根地址，用于拼接分享链接，如 https://app.example.com
     * 末尾斜杠可有可无
     */
    private String baseUrl = "http://localhost:3000";
}
