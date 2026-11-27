package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话历史消息
 */
@Data
@Builder
@Schema(description = "AI 会话消息")
public class ConversationMessageDTO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "百炼会话 ID")
    private String sessionId;

    @Schema(description = "用户提问")
    private String question;

    @Schema(description = "模型回答")
    private String answer;

    @Schema(description = "本轮附件元数据")
    private List<ChatAttachmentDTO> attachments;

    @Schema(description = "百炼请求 ID")
    private String requestId;

    @Schema(description = "结束原因")
    private String finishReason;

    @Schema(description = "输入 token")
    private Integer inputTokens;

    @Schema(description = "输出 token")
    private Integer outputTokens;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
