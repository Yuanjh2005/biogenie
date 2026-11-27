-- 已有数据库升级脚本：管理员授权信息、审计日志
ALTER TABLE `t_user`
    ADD COLUMN `admin_granted_by` BIGINT DEFAULT NULL COMMENT '管理员授权人ID' AFTER `role`,
    ADD COLUMN `admin_granted_at` DATETIME DEFAULT NULL COMMENT '管理员授权时间' AFTER `admin_granted_by`,
    ADD COLUMN `admin_remark` VARCHAR(255) DEFAULT NULL COMMENT '管理员授权备注' AFTER `admin_granted_at`;

CREATE TABLE IF NOT EXISTS `t_admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `operator_id` BIGINT NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `target_type` VARCHAR(32) DEFAULT NULL,
    `target_id` VARCHAR(128) DEFAULT NULL,
    `detail` VARCHAR(1000) DEFAULT NULL,
    `ip` VARCHAR(64) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_operator_create` (`operator_id`, `create_time`),
    KEY `idx_action_create` (`action`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计';

CREATE TABLE IF NOT EXISTS `t_api_behavior_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `user_role` VARCHAR(16) NOT NULL,
    `request_method` VARCHAR(8) NOT NULL,
    `request_path` VARCHAR(512) NOT NULL,
    `status_code` INT NOT NULL,
    `duration_ms` BIGINT NOT NULL,
    `ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_create` (`user_id`, `create_time`),
    KEY `idx_role_create` (`user_role`, `create_time`),
    KEY `idx_path_create` (`request_path`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用行为日志';
