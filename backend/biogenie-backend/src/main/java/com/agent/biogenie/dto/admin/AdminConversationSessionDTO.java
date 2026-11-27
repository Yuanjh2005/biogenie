package com.agent.biogenie.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理端用户会话列表项。 */
@Data
@Schema(description = "管理端用户会话列表项")
public class AdminConversationSessionDTO {

    private String sessionId;
    private Long userId;
    private String userNickname;

    @Schema(description = "脱敏手机号")
    private String userPhone;

    private String title;
    private String appKey;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
