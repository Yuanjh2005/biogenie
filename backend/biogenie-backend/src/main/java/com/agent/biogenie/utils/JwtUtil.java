package com.agent.biogenie.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JWT 工具类（Access / Refresh 双 Token）
 */
@Slf4j
@Component
public class JwtUtil {

    public static final String CLAIM_TYP = "typ";
    public static final String CLAIM_SID = "sid";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final Set<String> STANDARD_CLAIM_NAMES = Set.of(
            Claims.SUBJECT, Claims.ISSUED_AT, Claims.EXPIRATION,
            Claims.NOT_BEFORE, Claims.ID, Claims.ISSUER, Claims.AUDIENCE,
            CLAIM_TYP
    );

    private final SecretKey secretKey;

    @Getter
    private final long accessExpiration;

    @Getter
    private final long refreshExpiration;

    @Getter
    private final long renewThreshold;

    @Getter
    private final long gracePeriod;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration}") long accessExpiration,
                   @Value("${jwt.refresh-expiration}") long refreshExpiration,
                   @Value("${jwt.renew-threshold}") long renewThreshold,
                   @Value("${jwt.grace-period}") long gracePeriod) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.renewThreshold = renewThreshold;
        this.gracePeriod = gracePeriod;
    }

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(Long userId, Map<String, Object> claims) {
        return generateToken(userId, claims, TOKEN_TYPE_ACCESS, accessExpiration);
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(Long userId, Map<String, Object> claims) {
        return generateToken(userId, claims, TOKEN_TYPE_REFRESH, refreshExpiration);
    }

    private String generateToken(Long userId, Map<String, Object> claims, String typ, long expirationMillis) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expirationMillis);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expireDate)
                .claim(CLAIM_TYP, typ);

        if (claims != null && !claims.isEmpty()) {
            claims.forEach((key, value) -> {
                if (value != null && !STANDARD_CLAIM_NAMES.contains(key) && !CLAIM_TYP.equals(key)) {
                    builder.claim(key, value);
                }
            });
        }

        return builder.signWith(secretKey).compact();
    }

    /**
     * 从 Token 中解析 Claims（过期则抛异常）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析 Claims，允许已过期（签名仍须正确）
     */
    public Claims parseTokenAllowExpired(String token) {
        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserId(String token) {
        String subject = parseToken(token).getSubject();
        return Long.valueOf(subject);
    }

    /**
     * 从已解析的 Claims 中获取用户ID
     */
    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从 Token 中获取会话ID
     */
    public String getSessionId(String token) {
        return getSessionId(parseToken(token));
    }

    public String getSessionId(Claims claims) {
        Object sid = claims.get(CLAIM_SID);
        return sid == null ? null : String.valueOf(sid);
    }

    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(claims));
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(claims));
    }

    public String getTokenType(Claims claims) {
        Object typ = claims.get(CLAIM_TYP);
        // 兼容旧 Token（无 typ）：视为 access
        return typ == null ? TOKEN_TYPE_ACCESS : String.valueOf(typ);
    }

    /**
     * 剩余有效期（毫秒），已过期则为负数
     */
    public long getRemainingMillis(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return Long.MIN_VALUE;
        }
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * Access 是否处于可无感续期阈值（剩余 &lt; renewThreshold）
     */
    public boolean shouldRenewAccess(Claims claims) {
        long remaining = getRemainingMillis(claims);
        return remaining >= 0 && remaining < renewThreshold;
    }

    /**
     * Access 已过期但仍在宽限期内
     */
    public boolean isWithinGracePeriod(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return false;
        }
        long elapsedSinceExpire = System.currentTimeMillis() - expiration.getTime();
        return elapsedSinceExpire > 0 && elapsedSinceExpire <= gracePeriod;
    }

    /**
     * 复制业务 Claims（排除标准字段与 typ），用于续签
     */
    public Map<String, Object> copyBusinessClaims(Claims claims) {
        Map<String, Object> copied = new HashMap<>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            String key = entry.getKey();
            if (!STANDARD_CLAIM_NAMES.contains(key) && entry.getValue() != null) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断 Token 是否过期
     */
    public boolean isExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Access Token 过期时长（毫秒）
     */
    public long getExpiration() {
        return accessExpiration;
    }

    /**
     * Access Token 过期时间戳（毫秒）
     */
    public long getExpireTimeMillis() {
        return System.currentTimeMillis() + accessExpiration;
    }

    /**
     * Refresh Token 过期时间戳（毫秒）
     */
    public long getRefreshExpireTimeMillis() {
        return System.currentTimeMillis() + refreshExpiration;
    }
}
