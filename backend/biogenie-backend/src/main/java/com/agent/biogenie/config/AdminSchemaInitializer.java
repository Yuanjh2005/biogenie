package com.agent.biogenie.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 为存量环境幂等补齐新增的行为日志表。 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AdminSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计'
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用行为日志'
                """);
        ensureCreateTimeIndex("t_api_behavior_log");
        ensureCreateTimeIndex("t_admin_audit_log");
        log.info("管理日志表及时间索引已检查");
    }

    private void ensureCreateTimeIndex(String table) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = ? AND index_name = 'idx_create_time'
                    """, Integer.class, table);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD INDEX `idx_create_time` (`create_time`)");
            }
        } catch (Exception e) {
            log.warn("检查/创建日志时间索引失败: table={}, err={}", table, e.getMessage());
        }
    }
}
