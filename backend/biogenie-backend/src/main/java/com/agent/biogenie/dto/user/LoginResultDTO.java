package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录返回结果
 */
@Data
@Builder
@Schema(description = "登录返回结果")
public class LoginResultDTO {

    @Schema(description = "Access Token（兼容字段，等同 accessToken）")
    private String token;

    @Schema(description = "Refresh Token，用于换取新的 Access Token")
    private String refreshToken;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "当前用户在线设备台数")
    private int deviceCount;

    @Schema(description = "Access Token 过期时间(毫秒)")
    private long expireTime;

    @Schema(description = "Refresh Token 过期时间(毫秒)")
    private long refreshExpireTime;

    @Schema(description = "用户信息")
    private UserInfoDTO userInfo;

    @Schema(description = "SSO 登录后进入的模块；仅换票时传入启动信息才返回", example = "smart-drgf")
    private String targetModule;

    @Schema(description = "SSO 登录后自动发送的问题；仅换票时传入启动信息才返回")
    private String question;

    @Schema(description = "SSO 登录后自动发送的附件")
    private List<ChatAttachmentDTO> attachments;
}
