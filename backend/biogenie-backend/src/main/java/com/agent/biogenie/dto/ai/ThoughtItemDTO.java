package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 百炼思考过程条目（output.thoughts 元素）
 */
@Data
@Builder
@Schema(description = "AI 思考过程条目")
public class ThoughtItemDTO {

    @Schema(description = "动作标识，如 reasoning")
    private String action;

    @Schema(description = "思考内容片段")
    private String thought;

    @Schema(description = "动作类型，如 reasoning")
    private String actionType;

    @Schema(description = "思考响应片段")
    private String response;

    @Schema(description = "动作名称，如 思考过程")
    private String actionName;
}
