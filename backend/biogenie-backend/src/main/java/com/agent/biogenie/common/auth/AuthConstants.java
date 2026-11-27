package com.agent.biogenie.common.auth;

/**
 * 认证相关常量（Request Attribute 等）
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_SESSION_ID = "sessionId";
    public static final String ATTR_ROLE = "userRole";

    /** 自动续期后回写给前端的新 Access Token 响应头 */
    public static final String HEADER_ACCESS_TOKEN = "X-Access-Token";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    public static boolean isSuperAdmin(String role) {
        return ROLE_SUPER_ADMIN.equalsIgnoreCase(role);
    }

    public static boolean isAdminOrAbove(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_SUPER_ADMIN.equalsIgnoreCase(role);
    }
}
