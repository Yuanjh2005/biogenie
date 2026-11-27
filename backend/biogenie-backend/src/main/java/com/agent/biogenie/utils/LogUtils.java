package com.agent.biogenie.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 结构化日志工具类。不向日志写入用户标识（userId、手机号、openid 等）。
 * <p>
 * business / performance Logger 名称需与 logback-spring.xml 中保持一致
 */
public final class LogUtils {

    private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("com.agent.biogenie.business");
    private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger("com.agent.biogenie.performance");

    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION = "operation";

    private LogUtils() {
    }

    public static void setRequestContext(String requestId) {
        if (requestId != null) {
            MDC.put(REQUEST_ID, requestId);
        }
    }

    /**
     * @deprecated 不再将用户信息写入 MDC，请使用 {@link #setRequestContext(String)}
     */
    @Deprecated
    public static void setRequestContext(String requestId, String userId, String sessionId) {
        setRequestContext(requestId);
    }

    public static void clearRequestContext() {
        MDC.clear();
    }

    public static void logBusiness(String operation, String message, Object... args) {
        try {
            MDC.put(OPERATION, operation);
            BUSINESS_LOGGER.info(formatMessage(message, args));
        } finally {
            MDC.remove(OPERATION);
        }
    }

    /**
     * @deprecated 不再接收 userId，避免误写入用户信息
     */
    @Deprecated
    public static void logBusiness(String operation, String userId, String message, Object... args) {
        logBusiness(operation, message, args);
    }

    public static void logBusinessError(String operation, String message, Throwable throwable, Object... args) {
        try {
            MDC.put(OPERATION, operation);
            BUSINESS_LOGGER.error(formatMessage(message, args), throwable);
        } finally {
            MDC.remove(OPERATION);
        }
    }

    /**
     * @deprecated 不再接收 userId
     */
    @Deprecated
    public static void logBusinessError(String operation, String userId, String message,
                                        Throwable throwable, Object... args) {
        logBusinessError(operation, message, throwable, args);
    }

    public static void logUserOperation(String operation, String resource, String result) {
        logBusiness(operation, "用户操作 | resource=%s | result=%s", resource, result);
    }

    /**
     * @deprecated 不再接收 userId
     */
    @Deprecated
    public static void logUserOperation(String userId, String operation, String resource, String result) {
        logUserOperation(operation, resource, result);
    }

    public static void logApiCall(String method, String path, int statusCode, long duration) {
        logBusiness("API_CALL", "API调用 | %s %s | status=%d | duration=%dms", method, path, statusCode, duration);
    }

    /**
     * @deprecated 不再接收 userId
     */
    @Deprecated
    public static void logApiCall(String method, String path, String userId, int statusCode, long duration) {
        logApiCall(method, path, statusCode, duration);
    }

    public static void logFileOperation(String operation, String fileName, String result) {
        if ("FAILED".equalsIgnoreCase(result)) {
            logBusinessError(operation, "文件操作失败 | file=%s", null, fileName);
        }
    }

    /**
     * @deprecated 不再接收 userId / md5
     */
    @Deprecated
    public static void logFileOperation(String userId, String operation, String fileName,
                                        String fileMd5, String result) {
        logFileOperation(operation, fileName, result);
    }

    public static void logSystemStart(String component, String status, String details) {
        logBusiness("SYSTEM_START",
                "系统启动 | component=%s | status=%s | details=%s", component, status, details);
    }

    public static void logSystemError(String component, String error, Throwable throwable) {
        logBusinessError("SYSTEM_ERROR", "系统错误 | component=%s | error=%s", throwable, component, error);
    }

    public static void logPerformance(String operation, long duration, String details) {
        try {
            MDC.put(OPERATION, operation);
            PERFORMANCE_LOGGER.info("性能监控 | operation={} | duration={}ms | details={}",
                    operation, duration, details);
        } finally {
            MDC.remove(OPERATION);
        }
    }

    public static PerformanceMonitor startPerformanceMonitor(String operation) {
        return new PerformanceMonitor(operation);
    }

    static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return String.format(message, args);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder(message);
            for (Object arg : args) {
                sb.append(" | ").append(arg);
            }
            return sb.toString();
        }
    }

    public static class PerformanceMonitor {

        private final String operation;
        private final long startTime;

        private PerformanceMonitor(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
        }

        public long end(String details) {
            long duration = System.currentTimeMillis() - startTime;
            if (duration >= 3000) {
                logPerformance(operation, duration, details);
            }
            return duration;
        }
    }
}
