package com.agent.biogenie.module.auth.service.impl;

import com.agent.biogenie.dto.user.DeviceInfoDTO;
import com.agent.biogenie.dto.user.UserSessionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private UserSessionServiceImpl userSessionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userSessionService, "refreshExpiration", 604_800_000L);
        ReflectionTestUtils.setField(userSessionService, "redisTtlBuffer", 300_000L);
    }

    @Test
    void createSession_shouldStoreSessionAndAddToSet() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        DeviceInfoDTO deviceInfo = DeviceInfoDTO.builder()
                .deviceType("Web")
                .deviceName("Chrome on Windows")
                .build();

        String sessionId = userSessionService.createSession(1L, deviceInfo, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(sessionId);
        verify(hashOperations).putAll(startsWith("auth:session:1:"), anyMap());
        verify(redisTemplate).expire(startsWith("auth:session:1:"), eq(605_100_000L), eq(TimeUnit.MILLISECONDS));
        verify(setOperations).add(eq("auth:sessions:1"), eq(sessionId));
        verify(redisTemplate).expire(eq("auth:sessions:1"), eq(605_100_000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void validateSession_whenExists_shouldReturnTrue() {
        when(redisTemplate.hasKey("auth:session:1:abc")).thenReturn(true);

        assertTrue(userSessionService.validateSession(1L, "abc"));
    }

    @Test
    void validateSession_whenMissing_shouldReturnFalse() {
        when(redisTemplate.hasKey("auth:session:1:abc")).thenReturn(false);

        assertFalse(userSessionService.validateSession(1L, "abc"));
    }

    @Test
    void countSessions_shouldReturnSetSize() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("auth:sessions:1")).thenReturn(3L);

        assertEquals(3, userSessionService.countSessions(1L));
    }

    @Test
    void removeSession_shouldDeleteHashAndRemoveFromSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        userSessionService.removeSession(1L, "abc");

        verify(redisTemplate).delete("auth:session:1:abc");
        verify(setOperations).remove("auth:sessions:1", "abc");
    }

    @Test
    void removeAllSessions_shouldDeleteAllSessionKeysAndSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:sessions:1")).thenReturn(Set.of("sid-1", "sid-2"));

        userSessionService.removeAllSessions(1L);

        verify(redisTemplate).delete("auth:session:1:sid-1");
        verify(redisTemplate).delete("auth:session:1:sid-2");
        verify(redisTemplate).delete("auth:sessions:1");
    }

    @Test
    void removeAllSessions_whenNoSessions_shouldOnlyDeleteSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:sessions:1")).thenReturn(Set.of());

        userSessionService.removeAllSessions(1L);

        verify(redisTemplate, never()).delete(startsWith("auth:session:1:"));
        verify(redisTemplate).delete("auth:sessions:1");
    }

    @Test
    void listSessions_shouldReturnSessionDetails() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(setOperations.members("auth:sessions:1")).thenReturn(Set.of("sid-1", "sid-2"));

        Map<Object, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("sessionId", "sid-1");
        sessionData.put("deviceType", "Web");
        sessionData.put("deviceName", "Chrome on Windows");
        sessionData.put("clientIp", "127.0.0.1");
        sessionData.put("loginTime", "2026-07-17 10:00:00");
        sessionData.put("lastActiveTime", "2026-07-17 10:05:00");

        when(hashOperations.entries("auth:session:1:sid-1")).thenReturn(sessionData);
        when(hashOperations.entries("auth:session:1:sid-2")).thenReturn(Map.of());

        var sessions = userSessionService.listSessions(1L, "sid-1");

        assertEquals(1, sessions.size());
        UserSessionDTO current = sessions.get(0);
        assertEquals("sid-1", current.getSessionId());
        assertEquals("Web", current.getDeviceType());
        assertTrue(current.getCurrent());
        verify(setOperations).remove("auth:sessions:1", "sid-2");
    }

    @Test
    void touchSession_shouldUpdateLastActiveTimeWhenStale() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("auth:session:1:abc", "lastActiveTime"))
                .thenReturn("2026-01-01 00:00:00");

        userSessionService.touchSession(1L, "abc");

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations).put(eq("auth:session:1:abc"), eq("lastActiveTime"), valueCaptor.capture());
        assertNotNull(valueCaptor.getValue());
    }
}
