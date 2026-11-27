package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSO ticket 换取结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SSO ticket 换取结果")
public class SsoTicketResultDTO {

    @Schema(description = "一次性 ticket，用于前端免登")
    private String ticket;

    @Schema(description = "外部用户标识（请求传入或服务端自动生成的 UUID）")
    private String externalUserId;

    @Schema(description = "有效期（秒）", example = "300")
    private Long expireIn;
}
