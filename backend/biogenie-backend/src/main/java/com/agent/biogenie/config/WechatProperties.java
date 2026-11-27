package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信开放平台 - 网站应用（PC端扫码登录）配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    /** 网站应用 AppID */
    private String appId;

    /** 网站应用 AppSecret */
    private String appSecret;

    /** PC端扫码登录授权回调地址，需与开放平台「授权回调域」一致 */
    private String redirectUri;
}
