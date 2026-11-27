package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * AI 流式对话 SSE 事件
 */
@Data
@Builder
@Schema(description = "AI 流式对话事件")
public class ChatStreamEventDTO {

    @Schema(description = "事件类型: delta=增量文本, done=结束, error=错误")
    private String type;

    @Schema(description = "增量或完整文本片段")
    private String text;

    @Schema(description = "思考过程增量（开启 hasThoughts 时返回）")
    private List<ThoughtItemDTO> thoughts;

    @Schema(description = "回答引用的网页或文档来源")
    private List<DocReferenceDTO> docReferences;

    @Schema(description = "多轮会话 ID（通常在 done 时带回）")
    private String sessionId;

    @Schema(description = "结束原因，stop 表示正常结束；进行中为 null")
    private String finishReason;

    @Schema(description = "上游请求 ID")
    private String requestId;

    @Schema(description = "输入 token")
    private Integer inputTokens;

    @Schema(description = "输出 token")
    private Integer outputTokens;

    @Schema(description = "错误信息（type=error 时）")
    private String message;
}
