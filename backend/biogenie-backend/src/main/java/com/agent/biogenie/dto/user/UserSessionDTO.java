package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户登录会话信息
 */
@Data
@Builder
@Schema(description = "用户登录会话信息")
public class UserSessionDTO {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "登录时间")
    private String loginTime;

    @Schema(description = "最后活跃时间")
    private String lastActiveTime;

    @Schema(description = "是否为当前会话")
    private Boolean current;
}
