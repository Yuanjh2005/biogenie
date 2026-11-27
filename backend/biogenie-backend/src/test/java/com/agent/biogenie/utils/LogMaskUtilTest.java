package com.agent.biogenie.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogMaskUtilTest {

    @Test
    void maskPhone_shouldMaskMiddleDigits() {
        assertEquals("138****8000", LogMaskUtil.maskPhone("13800138000"));
    }

    @Test
    void maskIp_shouldMaskLastTwoOctets() {
        assertEquals("192.168.*.*", LogMaskUtil.maskIp("192.168.1.100"));
    }

    @Test
    void maskUserId_shouldMaskPhoneOnly() {
        assertEquals("138****8000", LogMaskUtil.maskUserId("13800138000"));
        assertEquals("12345", LogMaskUtil.maskUserId("12345"));
    }
}
