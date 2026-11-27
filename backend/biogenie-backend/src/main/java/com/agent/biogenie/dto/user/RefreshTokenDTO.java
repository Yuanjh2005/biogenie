package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 刷新请求
 */
@Data
@Schema(description = "Token 刷新请求")
public class RefreshTokenDTO {

    @NotBlank(message = "refreshToken 不能为空")
    @Schema(description = "Refresh Token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    @Override
    public String toString() {
        return "RefreshTokenDTO(refreshToken=***)";
    }
}
