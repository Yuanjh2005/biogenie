package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.config.AdminLogProperties;
import com.agent.biogenie.module.admin.mapper.AdminAuditLogMapper;
import com.agent.biogenie.module.admin.mapper.ApiBehaviorLogMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LogCleanupServiceTest {
    @Test
    void behaviorCleanupShouldDeleteInBatchesUntilShortBatch() {
        ApiBehaviorLogMapper behavior = mock(ApiBehaviorLogMapper.class);
        AdminAuditLogMapper audit = mock(AdminAuditLogMapper.class);
        AdminLogProperties properties = new AdminLogProperties();
        properties.setCleanupBatchSize(5000);
        properties.setCleanupMaxBatches(10);
        when(behavior.deleteExpired(any(LocalDateTime.class), eq(5000))).thenReturn(5000, 12);

        new LogCleanupService(behavior, audit, properties).cleanupBehaviorLogs();

        verify(behavior, times(2)).deleteExpired(any(LocalDateTime.class), eq(5000));
        verifyNoInteractions(audit);
    }
}
