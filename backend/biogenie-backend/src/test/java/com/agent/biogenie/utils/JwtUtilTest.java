package com.agent.biogenie.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = java.util.UUID.randomUUID().toString();
    private static final long ACCESS_EXPIRATION = 3_600_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;
    private static final long RENEW_THRESHOLD = 300_000L;
    private static final long GRACE_PERIOD = 600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION, RENEW_THRESHOLD, GRACE_PERIOD);
    }

    @Test
    void generateAccessToken_shouldBeValid() {
        String token = jwtUtil.generateAccessToken(1L, Map.of("phone", "13800138000", "sid", "session-abc"));
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("session-abc", jwtUtil.getSessionId(token));
        assertFalse(jwtUtil.isExpired(token));

        Claims claims = jwtUtil.parseToken(token);
        assertTrue(jwtUtil.isAccessToken(claims));
        assertFalse(jwtUtil.isRefreshToken(claims));
    }

    @Test
    void generateRefreshToken_shouldHaveRefreshType() {
        String token = jwtUtil.generateRefreshToken(1L, Map.of("sid", "session-abc"));
        Claims claims = jwtUtil.parseToken(token);
        assertTrue(jwtUtil.isRefreshToken(claims));
        assertEquals("session-abc", jwtUtil.getSessionId(claims));
    }

    @Test
    void getExpiration_shouldMatchAccessConfig() {
        assertEquals(ACCESS_EXPIRATION, jwtUtil.getExpiration());
        assertEquals(ACCESS_EXPIRATION, jwtUtil.getAccessExpiration());
        assertEquals(REFRESH_EXPIRATION, jwtUtil.getRefreshExpiration());
    }

    @Test
    void getExpireTimeMillis_shouldBeFutureTimestamp() {
        long before = System.currentTimeMillis();
        long expireTime = jwtUtil.getExpireTimeMillis();
        assertTrue(expireTime >= before + ACCESS_EXPIRATION);
        assertTrue(expireTime <= before + ACCESS_EXPIRATION + 1000);
    }

    @Test
    void shouldRenewAccess_whenRemainingBelowThreshold() {
        JwtUtil shortLived = new JwtUtil(SECRET, 60_000L, REFRESH_EXPIRATION, 300_000L, GRACE_PERIOD);
        String token = shortLived.generateAccessToken(1L, Map.of("sid", "s1"));
        Claims claims = shortLived.parseToken(token);
        assertTrue(shortLived.shouldRenewAccess(claims));
    }

    @Test
    void copyBusinessClaims_shouldKeepSidAndDropTyp() {
        String token = jwtUtil.generateAccessToken(1L, Map.of("sid", "s1", "phone", "13800138000"));
        Claims claims = jwtUtil.parseToken(token);
        Map<String, Object> copied = jwtUtil.copyBusinessClaims(claims);
        assertEquals("s1", copied.get("sid"));
        assertEquals("13800138000", copied.get("phone"));
        assertFalse(copied.containsKey(JwtUtil.CLAIM_TYP));
        assertFalse(copied.containsKey(Claims.SUBJECT));
    }
}
