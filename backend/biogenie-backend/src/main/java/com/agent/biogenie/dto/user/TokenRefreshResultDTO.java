package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Token 刷新结果
 */
@Data
@Builder
@Schema(description = "Token 刷新结果")
public class TokenRefreshResultDTO {

    @Schema(description = "新的 Access Token")
    private String token;

    @Schema(description = "Access Token 过期时间(毫秒)")
    private long expireTime;
}
