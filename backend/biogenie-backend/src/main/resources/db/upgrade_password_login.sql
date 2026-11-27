-- 已有数据库启用账号密码登录时执行一次（MySQL 8.0）
ALTER TABLE `t_user`
    ADD COLUMN `username` VARCHAR(32) DEFAULT NULL COMMENT '登录账号（小写）' AFTER `phone`,
    ADD COLUMN `password_hash` VARCHAR(64) DEFAULT NULL COMMENT 'PBKDF2-HMAC-SHA256摘要（Base64）' AFTER `username`,
    ADD COLUMN `password_salt` CHAR(36) DEFAULT NULL COMMENT 'UUID盐值' AFTER `password_hash`,
    ADD UNIQUE KEY `uk_username` (`username`);
