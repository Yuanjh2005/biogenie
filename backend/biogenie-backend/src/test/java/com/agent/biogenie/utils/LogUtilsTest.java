package com.agent.biogenie.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class LogUtilsTest {

    @AfterEach
    void tearDown() {
        LogUtils.clearRequestContext();
    }

    @Test
    void setRequestContext_shouldNotPutUserInfoInMdc() {
        LogUtils.setRequestContext("req123", "13800138000", "session1");

        assertEquals("req123", MDC.get(LogUtils.REQUEST_ID));
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("sessionId"));
    }

    @Test
    void clearRequestContext_shouldRemoveMdc() {
        LogUtils.setRequestContext("req123");
        LogUtils.clearRequestContext();

        assertNull(MDC.get(LogUtils.REQUEST_ID));
    }

    @Test
    void performanceMonitor_shouldCalculateDuration() throws InterruptedException {
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("TEST_OP");
        Thread.sleep(10);
        long duration = monitor.end("detail");

        assertTrue(duration >= 10);
    }

    @Test
    void formatMessage_shouldFormatArgs() {
        assertEquals("hello world", LogUtils.formatMessage("hello %s", "world"));
    }
}
