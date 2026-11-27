package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项
 */
@Data
@Builder
@Schema(description = "AI 会话列表项")
public class ConversationSessionDTO {

    @Schema(description = "百炼会话 ID")
    private String sessionId;

    @Schema(description = "归属用户 ID")
    private Long userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "AI 应用配置名")
    private String appKey;

    @Schema(description = "状态: 1=ACTIVE 0=ARCHIVED")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;
}
