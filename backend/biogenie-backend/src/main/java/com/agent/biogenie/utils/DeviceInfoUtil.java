package com.agent.biogenie.utils;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.dto.user.DeviceInfoDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 设备信息解析工具
 */
public final class DeviceInfoUtil {

    private static final String HEADER_DEVICE_TYPE = "X-Device-Type";
    private static final String HEADER_DEVICE_NAME = "X-Device-Name";

    private DeviceInfoUtil() {
    }

    public static DeviceInfoDTO resolve(HttpServletRequest request) {
        String deviceType = StrUtil.trim(request.getHeader(HEADER_DEVICE_TYPE));
        String deviceName = StrUtil.trim(request.getHeader(HEADER_DEVICE_NAME));
        String userAgent = StrUtil.blankToDefault(request.getHeader("User-Agent"), "Unknown");

        if (StrUtil.isBlank(deviceType)) {
            deviceType = parseDeviceType(userAgent);
        }
        if (StrUtil.isBlank(deviceName)) {
            deviceName = parseDeviceName(userAgent);
        }

        return DeviceInfoDTO.builder()
                .deviceType(deviceType)
                .deviceName(deviceName)
                .build();
    }

    public static String getUserAgent(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("User-Agent"), "Unknown");
    }

    private static String parseDeviceType(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "Mac";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Web";
    }

    private static String parseDeviceName(String userAgent) {
        String browser = "Unknown Browser";
        String os = "Unknown OS";
        String ua = userAgent;

        if (ua.contains("Edg/")) {
            browser = "Edge";
        } else if (ua.contains("Chrome/")) {
            browser = "Chrome";
        } else if (ua.contains("Firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("Safari/") && !ua.contains("Chrome/")) {
            browser = "Safari";
        }

        if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("Mac OS")) {
            os = "Mac";
        } else if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("iPhone") || ua.contains("iPad")) {
            os = "iOS";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        }

        return browser + " on " + os;
    }
}
