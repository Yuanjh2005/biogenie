package com.agent.biogenie.utils;

import com.agent.biogenie.dto.user.DeviceInfoDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceInfoUtilTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void resolve_shouldUseHeadersWhenPresent() {
        when(request.getHeader("X-Device-Type")).thenReturn("iOS");
        when(request.getHeader("X-Device-Name")).thenReturn("iPhone 15");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        DeviceInfoDTO deviceInfo = DeviceInfoUtil.resolve(request);

        assertEquals("iOS", deviceInfo.getDeviceType());
        assertEquals("iPhone 15", deviceInfo.getDeviceName());
    }

    @Test
    void resolve_shouldFallbackToUserAgentWhenHeadersMissing() {
        when(request.getHeader("X-Device-Type")).thenReturn(null);
        when(request.getHeader("X-Device-Name")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        DeviceInfoDTO deviceInfo = DeviceInfoUtil.resolve(request);

        assertEquals("Windows", deviceInfo.getDeviceType());
        assertTrue(deviceInfo.getDeviceName().contains("Chrome"));
        assertTrue(deviceInfo.getDeviceName().contains("Windows"));
    }
}
