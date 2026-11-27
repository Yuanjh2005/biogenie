package com.agent.biogenie.config;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.utils.LogUtils;
import com.agent.biogenie.common.auth.AuthConstants;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import com.agent.biogenie.module.admin.service.ApiBehaviorLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP 请求日志拦截器：仅记录异常，不写入用户标识
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "logStartTime";
    private static final String REQUEST_ID_ATTR = "requestId";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000;
    private final ApiBehaviorLogService behaviorLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        request.setAttribute(REQUEST_ID_ATTR, requestId);
        LogUtils.setRequestContext(requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
            String method = request.getMethod();
            String path = request.getRequestURI();
            int statusCode = response.getStatus();

            if (!isApiRequest(request)) {
                return;
            }
            if (!isLogManagementRequest(path)) {
                saveBehaviorLog(request, method, path, statusCode, duration);
            }
            if (ex != null) {
                log.error("请求异常 | {} {} | status={} | reason={}",
                        method, path, statusCode, ex.getMessage(), ex);
            }
            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                LogUtils.logPerformance("SLOW_REQUEST", duration,
                        String.format("%s %s | status=%d", method, path, statusCode));
            }
        } finally {
            LogUtils.clearRequestContext();
        }
    }

    private void saveBehaviorLog(HttpServletRequest request, String method, String path,
                                 int statusCode, long duration) {
        Object userId = request.getAttribute(AuthConstants.ATTR_USER_ID);
        if (!(userId instanceof Long)) {
            return;
        }
        ApiBehaviorLog row = new ApiBehaviorLog();
        row.setUserId((Long) userId);
        row.setUserRole(String.valueOf(request.getAttribute(AuthConstants.ATTR_ROLE)));
        row.setRequestMethod(method);
        row.setRequestPath(path);
        row.setStatusCode(statusCode);
        row.setDurationMs(duration);
        row.setIp(clientIp(request));
        row.setUserAgent(StrUtil.maxLength(request.getHeader("User-Agent"), 512));
        behaviorLogService.record(row);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip)) ip = request.getHeader("X-Real-IP");
        if (StrUtil.isBlank(ip)) ip = request.getRemoteAddr();
        return ip != null && ip.contains(",") ? ip.substring(0, ip.indexOf(',')).trim() : ip;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/");
    }

    private boolean isLogManagementRequest(String path) {
        return path.startsWith("/api/admin/behavior-logs")
                || path.startsWith("/api/admin/audit-logs");
    }
}
