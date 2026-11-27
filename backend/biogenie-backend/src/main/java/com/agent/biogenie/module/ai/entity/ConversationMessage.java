package com.agent.biogenie.module.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话消息（一轮问答）
 */
@Data
@TableName("t_conversation_message")
public class ConversationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private String question;

    private String answer;

    private String referenceMappingsJson;

    /** 本轮附件元数据 JSON：[{url,name,contentType}] */
    private String attachmentsJson;

    private String requestId;

    private String finishReason;

    private Integer inputTokens;

    private Integer outputTokens;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
