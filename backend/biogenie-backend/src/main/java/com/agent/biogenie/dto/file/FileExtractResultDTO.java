package com.agent.biogenie.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件文本抽取结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件文本抽取结果")
public class FileExtractResultDTO {

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "检测到的内容类型")
    private String contentType;

    @Schema(description = "抽取文本字符数（截断后）")
    private int charCount;

    @Schema(description = "是否因超长被截断")
    private boolean truncated;

    @Schema(description = "抽取的纯文本")
    private String text;
}
