-- BioGenie 数据库初始化脚本（MySQL 8.0）
-- 已合并原 migration：V2_conversation / V4_hot_question / V5_conversation_share / V6_user_sso
-- 适用于全新建库；已有库请按需手工对照变更，勿重复执行会冲突的 DDL。

CREATE DATABASE IF NOT EXISTS `biogenie_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `biogenie_db`;

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信OpenID',
    `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `username` VARCHAR(32) DEFAULT NULL COMMENT '登录账号（小写）',
    `password_hash` VARCHAR(64) DEFAULT NULL COMMENT 'PBKDF2-HMAC-SHA256摘要（Base64）',
    `password_salt` CHAR(36) DEFAULT NULL COMMENT 'UUID盐值',
    `external_id` VARCHAR(128) DEFAULT NULL COMMENT '外部系统用户ID',
    `source` VARCHAR(64) DEFAULT NULL COMMENT '外部系统来源标识',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `role` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色: USER普通用户 ADMIN管理员 SUPER_ADMIN超级管理员',
    `admin_granted_by` BIGINT DEFAULT NULL COMMENT '管理员授权人ID',
    `admin_granted_at` DATETIME DEFAULT NULL COMMENT '管理员授权时间',
    `admin_remark` VARCHAR(255) DEFAULT NULL COMMENT '管理员授权备注',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    UNIQUE KEY `uk_unionid` (`unionid`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_source_external_id` (`source`, `external_id`),
    KEY `idx_deleted_create_time` (`deleted`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 管理员高危操作审计表
CREATE TABLE IF NOT EXISTS `t_admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(32) DEFAULT NULL COMMENT '目标类型',
    `target_id` VARCHAR(128) DEFAULT NULL COMMENT '目标ID',
    `detail` VARCHAR(1000) DEFAULT NULL COMMENT '脱敏详情',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_create` (`operator_id`, `create_time`),
    KEY `idx_action_create` (`action`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计';

-- 已登录用户 API 调用行为日志
CREATE TABLE IF NOT EXISTS `t_api_behavior_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '调用用户ID',
    `user_role` VARCHAR(16) NOT NULL COMMENT '调用时角色',
    `request_method` VARCHAR(8) NOT NULL COMMENT 'HTTP方法',
    `request_path` VARCHAR(512) NOT NULL COMMENT '请求路径（不记录查询参数）',
    `status_code` INT NOT NULL COMMENT 'HTTP状态码',
    `duration_ms` BIGINT NOT NULL COMMENT '耗时毫秒',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_create` (`user_id`, `create_time`),
    KEY `idx_role_create` (`user_role`, `create_time`),
    KEY `idx_path_create` (`request_path`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用行为日志';

-- 首个用户完成登录后，可按需授予超级管理员角色（用于管理账号、运营统计和审计）：
-- UPDATE `t_user` SET `role` = 'SUPER_ADMIN' WHERE `id` = 目标用户ID;

-- AI 会话表（业务键 = 百炼 session_id）
CREATE TABLE IF NOT EXISTS `t_conversation_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` VARCHAR(128) NOT NULL COMMENT '百炼会话ID',
    `user_id` BIGINT NOT NULL COMMENT '归属用户ID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '会话标题（首问截断）',
    `app_key` VARCHAR(64) DEFAULT NULL COMMENT 'AI应用配置名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=ACTIVE 0=ARCHIVED',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_status_update` (`user_id`, `status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- AI 问答明细表
CREATE TABLE IF NOT EXISTS `t_conversation_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` VARCHAR(128) NOT NULL COMMENT '百炼会话ID',
    `user_id` BIGINT NOT NULL COMMENT '归属用户ID',
    `question` TEXT NOT NULL COMMENT '用户提问',
    `answer` MEDIUMTEXT NOT NULL COMMENT '模型回答',
    `reference_mappings_json` LONGTEXT DEFAULT NULL COMMENT '引文映射JSON（预留）',
    `attachments_json` TEXT DEFAULT NULL COMMENT '本轮附件元数据JSON',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '百炼请求ID',
    `finish_reason` VARCHAR(32) DEFAULT NULL COMMENT '结束原因',
    `input_tokens` INT DEFAULT NULL COMMENT '输入token数',
    `output_tokens` INT DEFAULT NULL COMMENT '输出token数',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_create` (`session_id`, `create_time`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';

-- AI 热门问题统计表
CREATE TABLE IF NOT EXISTS `t_hot_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_key` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '应用配置名，空串表示 global',
    `question_key` VARCHAR(512) NOT NULL COMMENT '归一化后的问题键',
    `display_question` VARCHAR(512) NOT NULL COMMENT '展示用原文（最近一次）',
    `hit_count` BIGINT NOT NULL DEFAULT 1 COMMENT '命中次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_question` (`app_key`, `question_key`),
    KEY `idx_app_hit` (`app_key`, `hit_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI热门问题统计表';

-- AI 对话分享快照表
CREATE TABLE IF NOT EXISTS `t_conversation_share` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `share_token` VARCHAR(32) NOT NULL COMMENT '分享令牌（链接用）',
    `user_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `session_id` VARCHAR(128) NOT NULL COMMENT '来源百炼会话ID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '分享标题',
    `snapshot_json` MEDIUMTEXT NOT NULL COMMENT '脱敏问答快照JSON',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1有效 0已失效（过期）',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_share_token` (`share_token`),
    KEY `idx_user_create` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话分享快照表';
