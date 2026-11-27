package com.agent.biogenie.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打开分享页内容（脱敏）
 */
@Data
@Builder
@Schema(description = "对话分享详情")
public class ConversationShareViewDTO {

    @Schema(description = "分享标题")
    private String title;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "快照消息列表")
    private List<ShareMessageDTO> messages;

    @Data
    @Builder
    @Schema(description = "分享快照中的一轮问答")
    public static class ShareMessageDTO {

        @Schema(description = "用户提问")
        private String question;

        @Schema(description = "模型回答")
        private String answer;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;

        @Schema(description = "附件元数据（无 url）")
        private List<ShareAttachmentDTO> attachments;
    }

    @Data
    @Builder
    @Schema(description = "分享附件（脱敏）")
    public static class ShareAttachmentDTO {

        @Schema(description = "原始文件名")
        private String name;

        @Schema(description = "MIME 类型")
        private String contentType;
    }
}
