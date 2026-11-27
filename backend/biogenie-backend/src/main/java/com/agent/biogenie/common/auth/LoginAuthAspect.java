package com.agent.biogenie.common.auth;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.module.auth.service.UserSessionService;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 登录鉴权切面：默认拦截所有业务 Controller，{@link Anonymous} 标记的接口放行。
 * Access 剩余不足阈值或处于宽限期时，通过响应头回写新的 Access Token。
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class LoginAuthAspect {

    private final JwtUtil jwtUtil;
    private final UserSessionService userSessionService;
    private final UserMapper userMapper;

    @Around("execution(* com.agent.biogenie.module..controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        if (isAnonymous(method, targetClass)) {
            return joinPoint.proceed();
        }

        ServletRequestAttributes attrs = currentRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return joinPoint.proceed();
        }

        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        Claims claims;
        boolean expired = false;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
            expired = true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        if (jwtUtil.isRefreshToken(claims)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请使用 Access Token 访问接口");
        }

        if (expired && !jwtUtil.isWithinGracePeriod(claims)) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserId(claims);
        String sessionId = jwtUtil.getSessionId(claims);
        if (StrUtil.isBlank(sessionId)) {
            throw new BizException(ResultCode.SESSION_INVALID);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            userSessionService.removeAllSessions(userId);
            throw new BizException(ResultCode.USER_DISABLED);
        }

        if (!userSessionService.validateSession(userId, sessionId)) {
            throw new BizException(ResultCode.SESSION_INVALID);
        }

        userSessionService.touchSession(userId, sessionId);
        request.setAttribute(AuthConstants.ATTR_USER_ID, userId);
        request.setAttribute(AuthConstants.ATTR_SESSION_ID, sessionId);
        request.setAttribute(AuthConstants.ATTR_ROLE,
                StrUtil.blankToDefault(user.getRole(), AuthConstants.ROLE_USER));

        if (expired || jwtUtil.shouldRenewAccess(claims)) {
            writeRenewedAccessToken(attrs.getResponse(), userId, claims, sessionId);
        }

        return joinPoint.proceed();
    }

    private void writeRenewedAccessToken(HttpServletResponse response, Long userId,
                                         Claims oldClaims, String sessionId) {
        if (response == null) {
            return;
        }
        Map<String, Object> renewClaims = jwtUtil.copyBusinessClaims(oldClaims);
        renewClaims.put(JwtUtil.CLAIM_SID, sessionId);
        String newAccessToken = jwtUtil.generateAccessToken(userId, renewClaims);
        response.setHeader(AuthConstants.HEADER_ACCESS_TOKEN, newAccessToken);
    }

    private boolean isAnonymous(Method method, Class<?> targetClass) {
        return AnnotationUtils.findAnnotation(method, Anonymous.class) != null
                || AnnotationUtils.findAnnotation(targetClass, Anonymous.class) != null;
    }

    private ServletRequestAttributes currentRequestAttributes() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return attrs;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return request.getHeader("token");
    }
}
