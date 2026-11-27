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
 * AI 对话分享快照
 */
@Data
@TableName("t_conversation_share")
public class ConversationShare implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_EXPIRED = 0;
    public static final int STATUS_ACTIVE = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shareToken;

    private Long userId;

    private String sessionId;

    private String title;

    private String snapshotJson;

    /** 状态: 1=有效 0=已失效（过期） */
    private Integer status;

    private LocalDateTime expireAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
