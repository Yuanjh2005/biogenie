package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话附件元数据（历史回显等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 对话附件")
public class ChatAttachmentDTO {

    @NotBlank(message = "附件 url 不能为空")
    @Schema(description = "文件公网 HTTPS URL")
    private String url;

    @Schema(description = "原始文件名")
    private String name;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "文件大小（字节）")
    private Long size;
}
