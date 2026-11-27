package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信开放平台 PC 端扫码登录请求
 */
@Data
@Schema(description = "微信PC端扫码登录请求")
public class WechatLoginDTO {

    @NotBlank(message = "微信授权码不能为空")
    @Schema(description = "扫码授权后回调 URL 中的 code 参数", example = "081xxy100")
    private String code;

    @NotBlank(message = "state 不能为空")
    @Schema(description = "扫码授权后回调 URL 中的 state 参数，需与获取二维码配置时返回的一致", example = "a1b2c3d4")
    private String state;
}
