package com.agent.biogenie.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信OpenID */
    private String openid;

    /** 微信UnionID */
    private String unionid;

    /** 手机号 */
    private String phone;

    /** 登录账号（统一按小写存储） */
    private String username;

    /** PBKDF2-HMAC-SHA256 密码摘要 */
    private String passwordHash;

    /** 每个账号独立的 UUID 盐值 */
    private String passwordSalt;

    /** 外部系统用户 ID */
    private String externalId;

    /** 外部系统来源标识 */
    private String source;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别: 0未知 1男 2女 */
    private Integer gender;

    /** 状态: 0禁用 1正常 */
    private Integer status;

    /** 角色: USER普通用户 ADMIN管理员 SUPER_ADMIN超级管理员 */
    private String role;

    /** 管理员授权人 */
    private Long adminGrantedBy;

    /** 管理员授权时间 */
    private LocalDateTime adminGrantedAt;

    /** 管理员授权备注 */
    private String adminRemark;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
