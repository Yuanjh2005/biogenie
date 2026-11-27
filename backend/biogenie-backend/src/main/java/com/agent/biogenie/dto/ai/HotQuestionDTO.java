package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热门问题条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热门问题")
public class HotQuestionDTO {

    @Schema(description = "展示用问题文案")
    private String question;

    @Schema(description = "命中次数")
    private Long hitCount;
}
