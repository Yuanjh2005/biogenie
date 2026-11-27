package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 百炼多轮对话请求
 */
@Data
@Schema(description = "AI 多轮对话请求")
public class ChatRequestDTO {

    @NotBlank(message = "prompt 不能为空")
    @Schema(description = "本轮用户输入", example = "请简单介绍一下 EGFR")
    private String prompt;

    @Schema(description = "多轮会话 ID；首轮不传，后续传上一轮响应中的 sessionId")
    private String sessionId;

    @Schema(description = "应用配置名：smartdrgf-fast / smartdrgf-web / design-mcp / deep-pubmed，默认 smartdrgf-fast",
            example = "smartdrgf-fast")
    private String app;

    @Schema(description = "是否启用深度思考模式，默认 false")
    private Boolean enableThinking;

    @Schema(description = "是否在响应中返回思考过程，须与 enableThinking=true 同时使用")
    private Boolean hasThoughts;

    @Valid
    @Schema(description = "本轮附件（公网 HTTPS URL）；图片进 image_list，其它进 file_list")
    private List<ChatAttachmentDTO> attachments;

}
