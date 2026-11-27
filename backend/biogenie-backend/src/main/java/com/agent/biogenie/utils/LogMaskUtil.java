package com.agent.biogenie.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 日志脱敏工具
 */
public final class LogMaskUtil {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    private LogMaskUtil() {
    }

    /**
     * 手机号脱敏：138****8000
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * IP 脱敏：192.168.*.*
     */
    public static String maskIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        if (ip.contains(":")) {
            int idx = ip.lastIndexOf(':');
            return idx > 0 ? ip.substring(0, idx) + ":*" : ip;
        }
        return ip;
    }

    /**
     * 用户标识脱敏：若为手机号则脱敏，否则原样返回
     */
    public static String maskUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return userId;
        }
        if (userId.matches(PHONE_PATTERN)) {
            return maskPhone(userId);
        }
        return userId;
    }
}
