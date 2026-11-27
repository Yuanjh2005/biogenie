package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 百炼多轮对话响应
 */
@Data
@Builder
@Schema(description = "AI 多轮对话响应")
public class ChatResultDTO {

    @Schema(description = "模型回答内容")
    private String text;

    @Schema(description = "思考过程（开启 hasThoughts 时返回）")
    private List<ThoughtItemDTO> thoughts;

    @Schema(description = "回答引用的网页或文档来源")
    private List<DocReferenceDTO> docReferences;

    @Schema(description = "本轮会话 ID，下一轮请求必须传入")
    private String sessionId;

    @Schema(description = "结束原因，stop 表示正常结束")
    private String finishReason;

    @Schema(description = "上游请求唯一 ID，报错排查用")
    private String requestId;

    @Schema(description = "本次输入消耗 token 数")
    private Integer inputTokens;

    @Schema(description = "本次输出消耗 token 数")
    private Integer outputTokens;
}
