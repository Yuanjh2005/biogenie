package com.agent.biogenie.module.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.dto.user.DeviceInfoDTO;
import com.agent.biogenie.dto.user.UserSessionDTO;
import com.agent.biogenie.module.auth.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户多端登录会话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String SESSIONS_SET_PREFIX = "auth:sessions:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_DEVICE_TYPE = "deviceType";
    private static final String FIELD_DEVICE_NAME = "deviceName";
    private static final String FIELD_CLIENT_IP = "clientIp";
    private static final String FIELD_USER_AGENT = "userAgent";
    private static final String FIELD_LOGIN_TIME = "loginTime";
    private static final String FIELD_LAST_ACTIVE_TIME = "lastActiveTime";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.redis-ttl-buffer}")
    private long redisTtlBuffer;

    @Override
    public String createSession(Long userId, DeviceInfoDTO deviceInfo, String clientIp, String userAgent) {
        String sessionId = IdUtil.simpleUUID();
        String now = formatNow();
        String sessionKey = buildSessionKey(userId, sessionId);
        String sessionsSetKey = buildSessionsSetKey(userId);

        Map<String, Object> sessionData = Map.of(
                FIELD_SESSION_ID, sessionId,
                FIELD_USER_ID, String.valueOf(userId),
                FIELD_DEVICE_TYPE, defaultDeviceType(deviceInfo),
                FIELD_DEVICE_NAME, defaultDeviceName(deviceInfo),
                FIELD_CLIENT_IP, StrUtil.blankToDefault(clientIp, "unknown"),
                FIELD_USER_AGENT, StrUtil.blankToDefault(userAgent, "Unknown"),
                FIELD_LOGIN_TIME, now,
                FIELD_LAST_ACTIVE_TIME, now
        );

        long sessionTtlMillis = sessionTtlMillis();
        redisTemplate.opsForHash().putAll(sessionKey, sessionData);
        redisTemplate.expire(sessionKey, sessionTtlMillis, TimeUnit.MILLISECONDS);

        redisTemplate.opsForSet().add(sessionsSetKey, sessionId);
        redisTemplate.expire(sessionsSetKey, sessionTtlMillis, TimeUnit.MILLISECONDS);

        return sessionId;
    }

    @Override
    public boolean validateSession(Long userId, String sessionId) {
        if (userId == null || StrUtil.isBlank(sessionId)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildSessionKey(userId, sessionId)));
    }

    @Override
    public void touchSession(Long userId, String sessionId) {
        if (userId == null || StrUtil.isBlank(sessionId)) {
            return;
        }

        String sessionKey = buildSessionKey(userId, sessionId);
        Object lastActiveObj = redisTemplate.opsForHash().get(sessionKey, FIELD_LAST_ACTIVE_TIME);
        if (lastActiveObj != null) {
            try {
                LocalDateTime lastActive = LocalDateTime.parse(String.valueOf(lastActiveObj), DATE_TIME_FORMATTER);
                if (lastActive.isAfter(LocalDateTime.now().minusMinutes(5))) {
                    return;
                }
            } catch (Exception e) {
                log.debug("解析 lastActiveTime 失败，继续更新: {}", e.getMessage());
            }
        }

        String now = formatNow();
        long sessionTtlMillis = sessionTtlMillis();
        redisTemplate.opsForHash().put(sessionKey, FIELD_LAST_ACTIVE_TIME, now);
        redisTemplate.expire(sessionKey, sessionTtlMillis, TimeUnit.MILLISECONDS);
        redisTemplate.expire(buildSessionsSetKey(userId), sessionTtlMillis, TimeUnit.MILLISECONDS);
    }

    private long sessionTtlMillis() {
        return refreshExpiration + redisTtlBuffer;
    }

    @Override
    public List<UserSessionDTO> listSessions(Long userId, String currentSessionId) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(buildSessionsSetKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSessionDTO> sessions = new ArrayList<>();
        for (Object sessionIdObj : sessionIds) {
            String sessionId = String.valueOf(sessionIdObj);
            UserSessionDTO session = getSession(userId, sessionId, currentSessionId);
            if (session != null) {
                sessions.add(session);
            } else {
                redisTemplate.opsForSet().remove(buildSessionsSetKey(userId), sessionId);
            }
        }
        return sessions;
    }

    @Override
    public int countSessions(Long userId) {
        Long count = redisTemplate.opsForSet().size(buildSessionsSetKey(userId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        if (userId == null || StrUtil.isBlank(sessionId)) {
            return;
        }
        redisTemplate.delete(buildSessionKey(userId, sessionId));
        redisTemplate.opsForSet().remove(buildSessionsSetKey(userId), sessionId);
    }

    @Override
    public void removeAllSessions(Long userId) {
        if (userId == null) {
            return;
        }
        String sessionsSetKey = buildSessionsSetKey(userId);
        Set<Object> sessionIds = redisTemplate.opsForSet().members(sessionsSetKey);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            for (Object sessionIdObj : sessionIds) {
                redisTemplate.delete(buildSessionKey(userId, String.valueOf(sessionIdObj)));
            }
        }
        redisTemplate.delete(sessionsSetKey);
    }

    private UserSessionDTO getSession(Long userId, String sessionId, String currentSessionId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(buildSessionKey(userId, sessionId));
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        return UserSessionDTO.builder()
                .sessionId(getField(entries, FIELD_SESSION_ID, sessionId))
                .deviceType(getField(entries, FIELD_DEVICE_TYPE, "Unknown"))
                .deviceName(getField(entries, FIELD_DEVICE_NAME, "Unknown"))
                .clientIp(getField(entries, FIELD_CLIENT_IP, "unknown"))
                .loginTime(getField(entries, FIELD_LOGIN_TIME, null))
                .lastActiveTime(getField(entries, FIELD_LAST_ACTIVE_TIME, null))
                .current(sessionId.equals(currentSessionId))
                .build();
    }

    private String getField(Map<Object, Object> entries, String field, String defaultValue) {
        Object value = entries.get(field);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String buildSessionKey(Long userId, String sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private String buildSessionsSetKey(Long userId) {
        return SESSIONS_SET_PREFIX + userId;
    }

    private String formatNow() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private String defaultDeviceType(DeviceInfoDTO deviceInfo) {
        if (deviceInfo == null || StrUtil.isBlank(deviceInfo.getDeviceType())) {
            return "Unknown";
        }
        return deviceInfo.getDeviceType();
    }

    private String defaultDeviceName(DeviceInfoDTO deviceInfo) {
        if (deviceInfo == null || StrUtil.isBlank(deviceInfo.getDeviceName())) {
            return "Unknown Device";
        }
        return deviceInfo.getDeviceName();
    }
}
