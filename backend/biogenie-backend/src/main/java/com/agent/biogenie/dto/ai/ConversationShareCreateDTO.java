package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建分享结果
 */
@Data
@Builder
@Schema(description = "创建对话分享结果")
public class ConversationShareCreateDTO {

    @Schema(description = "分享令牌")
    private String shareToken;

    @Schema(description = "完整分享链接")
    private String shareUrl;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "分享标题")
    private String title;
}
