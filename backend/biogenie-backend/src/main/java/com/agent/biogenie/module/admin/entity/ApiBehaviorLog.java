package com.agent.biogenie.module.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_api_behavior_log")
public class ApiBehaviorLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userRole;
    private String requestMethod;
    private String requestPath;
    private Integer statusCode;
    private Long durationMs;
    private String ip;
    private String userAgent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
