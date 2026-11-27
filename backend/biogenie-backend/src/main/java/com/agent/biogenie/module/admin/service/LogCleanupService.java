package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.config.AdminLogProperties;
import com.agent.biogenie.module.admin.mapper.AdminAuditLogMapper;
import com.agent.biogenie.module.admin.mapper.ApiBehaviorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.IntSupplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogCleanupService {
    private final ApiBehaviorLogMapper behaviorMapper;
    private final AdminAuditLogMapper auditMapper;
    private final AdminLogProperties properties;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Shanghai")
    public void cleanupBehaviorLogs() {
        LocalDateTime expireTime = LocalDateTime.now()
                .minusDays(Math.max(1, properties.getBehaviorRetentionDays()));
        cleanup("API 行为日志", () -> behaviorMapper.deleteExpired(
                expireTime, safeBatchSize()));
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Shanghai")
    public void cleanupAuditLogs() {
        LocalDateTime expireTime = LocalDateTime.now()
                .minusDays(Math.max(1, properties.getAuditRetentionDays()));
        cleanup("管理员审计日志", () -> auditMapper.deleteExpired(
                expireTime, safeBatchSize()));
    }

    private void cleanup(String name, IntSupplier deleteBatch) {
        int total = 0;
        int maxBatches = Math.max(1, properties.getCleanupMaxBatches());
        for (int i = 0; i < maxBatches; i++) {
            int deleted = deleteBatch.getAsInt();
            total += deleted;
            if (deleted < safeBatchSize()) break;
        }
        if (total > 0) log.info("过期{} 清理完成: deleted={}", name, total);
    }

    private int safeBatchSize() {
        return Math.max(100, properties.getCleanupBatchSize());
    }
}
