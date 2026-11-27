package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 微信开放平台 PC 端扫码登录配置
 */
@Data
@Builder
@Schema(description = "微信PC端扫码登录配置")
public class WechatQrConfigDTO {

    @Schema(description = "微信开放平台网站应用 AppID")
    private String appId;

    @Schema(description = "授权回调地址（需与开放平台配置一致）")
    private String redirectUri;

    @Schema(description = "授权 scope，PC端固定为 snsapi_login")
    private String scope;

    @Schema(description = "防 CSRF 随机 state，登录时需原样回传")
    private String state;

    @Schema(description = "PC端扫码登录完整 URL，前端可直接用于展示二维码")
    private String qrConnectUrl;
}
