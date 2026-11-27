package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.config.AdminLogProperties;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import com.agent.biogenie.module.admin.mapper.ApiBehaviorLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ApiBehaviorLogServiceTest {
    @Test
    void recordAndFlushShouldUseOneBatchInsert() {
        ApiBehaviorLogMapper mapper = mock(ApiBehaviorLogMapper.class);
        AdminLogProperties properties = new AdminLogProperties();
        properties.setBehaviorFlushSize(500);
        ApiBehaviorLogService service = new ApiBehaviorLogService(mapper, properties);
        ApiBehaviorLog first = row(1L);
        ApiBehaviorLog second = row(2L);

        service.record(first);
        service.record(second);
        assertEquals(2, service.queueSize());
        service.flush();

        ArgumentCaptor<List<ApiBehaviorLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertBatch(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertNotNull(first.getCreateTime());
        assertEquals(0, service.queueSize());
    }

    private ApiBehaviorLog row(Long id) {
        ApiBehaviorLog row = new ApiBehaviorLog();
        row.setUserId(id); row.setUserRole("USER"); row.setRequestMethod("GET");
        row.setRequestPath("/api/test"); row.setStatusCode(200); row.setDurationMs(1L);
        return row;
    }
}
