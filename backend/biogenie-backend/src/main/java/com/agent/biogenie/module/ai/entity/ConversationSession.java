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
 * AI 对话会话
 */
@Data
@TableName("t_conversation_session")
public class ConversationSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_ARCHIVED = 0;
    public static final int STATUS_ACTIVE = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 百炼会话 ID */
    private String sessionId;

    private Long userId;

    private String title;

    private String appKey;

    /** 状态: 1=ACTIVE 0=ARCHIVED */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
