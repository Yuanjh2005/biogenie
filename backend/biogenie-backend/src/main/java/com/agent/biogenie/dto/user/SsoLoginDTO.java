package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSO ticket 免登请求（前端调用）
 */
@Data
@Schema(description = "SSO ticket 免登请求")
public class SsoLoginDTO {

    @NotBlank(message = "ticket 不能为空")
    @Schema(description = "换票接口返回的一次性 ticket")
    private String ticket;

    @Override
    public String toString() {
        return "SsoLoginDTO(ticket=***)";
    }
}