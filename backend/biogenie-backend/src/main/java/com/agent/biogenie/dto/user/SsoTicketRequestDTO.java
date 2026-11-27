package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 外部系统换取 SSO ticket 请求（服务端调用）
 */
@Data
@Schema(description = "外部系统换取 SSO ticket 请求")
public class SsoTicketRequestDTO {

    @NotBlank(message = "clientId 不能为空")
    @Schema(description = "客户端 ID", example = "biogenie")
    private String clientId;

    @NotBlank(message = "clientSecret 不能为空")
    @Schema(description = "客户端密钥")
    private String clientSecret;

    @Size(max = 128, message = "externalUserId 长度不能超过 128")
    @Schema(description = "外部用户标识（可选；不传则由服务端生成 UUID）", example = "u-10001")
    private String externalUserId;

    @Pattern(regexp = "smart-drgf|deep-pubmed|design-mcp",
            message = "targetModule 必须是 smart-drgf、deep-pubmed 或 design-mcp")
    @Schema(description = "登录后进入的模块（传入 question 时必填；未传 question 和附件时将进入首页）", example = "smart-drgf")
    private String targetModule;

    @Size(max = 8000, message = "question 长度不能超过 8000")
    @Schema(description = "登录后自动发送的问题（可选；不传问题和附件时将进入首页）", example = "EGFR 与肺癌有什么关系？")
    private String question;

    @Size(max = 10, message = "attachmentIds 数量不能超过 10")
    @Schema(description = "通过 /api/auth/sso/attachment 暂存后返回的附件 ID；可选，最多 10 个，不能脱离 question 单独使用")
    private List<@NotBlank(message = "attachmentId 不能为空") String> attachmentIds;

    @Override
    public String toString() {
        return "SsoTicketRequestDTO(clientId=" + clientId + ", clientSecret=***, externalUserId="
                + externalUserId + ", targetModule=" + targetModule + ", question=***)";
    }
}
