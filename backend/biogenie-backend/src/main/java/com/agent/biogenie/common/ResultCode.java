package com.agent.biogenie.common;

import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
public enum ResultCode {

    // ==================== 通用状态码 ====================
    SUCCESS(20000, "操作成功"),
    FAIL(50000, "操作失败"),

    // ==================== 客户端错误 4xx ====================
    BAD_REQUEST(40000, "参数错误"),
    UNAUTHORIZED(40100, "未授权，请先登录"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    TOKEN_EXPIRED(40101, "Access Token已过期，请刷新后重试"),
    SESSION_INVALID(40102, "会话已失效，请重新登录"),

    // ==================== 服务端错误 5xx ====================
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ==================== 业务错误码 ====================
    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    USER_EXISTED(1003, "用户已存在"),
    USERNAME_OR_PASSWORD_ERROR(1004, "账号或密码错误"),

    // 验证码相关 2xxx
    SMS_CODE_ERROR(2001, "验证码错误"),
    SMS_CODE_EXPIRED(2002, "验证码已过期"),
    SMS_CODE_SEND_FAILED(2003, "短信发送失败"),
    SMS_CODE_FREQUENT(2004, "验证码发送过于频繁，请稍后再试"),
    SECOND_FACTOR_REQUIRED(2005, "该操作需要二次确认"),
    SECOND_FACTOR_INVALID(2006, "二次确认令牌无效或已过期"),

    // 文件相关 3xxx
    FILE_UPLOAD_FAILED(3001, "文件上传失败"),
    FILE_NOT_FOUND(3002, "文件不存在"),
    FILE_SIZE_EXCEEDED(3003, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(3004, "文件类型不支持"),

    // 微信相关 4xxx
    WECHAT_LOGIN_FAILED(4001, "微信登录失败"),
    WECHAT_CODE_INVALID(4002, "微信授权码无效"),

    // SSO 相关 6xxx
    SSO_CLIENT_INVALID(6001, "SSO 客户端凭证无效"),
    SSO_TICKET_INVALID(6002, "SSO ticket 无效或已过期"),

    // AI / 百炼相关 5xxx
    AI_CONFIG_ERROR(5001, "AI 应用配置错误"),
    AI_CALL_FAILED(5002, "AI 调用失败"),
    CONVERSATION_NOT_FOUND(5003, "对话会话不存在"),
    CONVERSATION_ARCHIVED(5004, "对话会话已归档"),
    SHARE_NOT_FOUND(5005, "分享不存在"),
    SHARE_EXPIRED(5006, "分享已过期");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
