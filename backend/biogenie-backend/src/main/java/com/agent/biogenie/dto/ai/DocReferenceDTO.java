package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 百炼回答引用来源（output.doc_references 元素）。
 */
@Data
@Builder
@Schema(description = "AI 回答引用来源")
public class DocReferenceDTO {

    @Schema(description = "回答中的引用编号，如 1 或 [1]")
    private String indexId;

    @Schema(description = "引用片段标题")
    private String title;

    @Schema(description = "来源文档 ID")
    private String docId;

    @Schema(description = "来源文档名称")
    private String docName;

    @Schema(description = "来源网页或文档地址")
    private String docUrl;

    @Schema(description = "被引用的文本片段")
    private String text;

    @Schema(description = "业务关联标识")
    private String bizId;

    @Schema(description = "引用图片地址")
    private List<String> images;
}
