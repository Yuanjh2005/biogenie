package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "SSO 启动附件暂存结果")
public class SsoAttachmentResultDTO {
    private String attachmentId;
    private String name;
    private String contentType;
    private long size;
    private long expireIn;
}
