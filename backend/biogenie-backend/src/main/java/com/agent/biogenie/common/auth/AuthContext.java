package com.agent.biogenie.common.auth;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前登录上下文工具
 */
public final class AuthContext {

    private AuthContext() {
    }

    public static Long getUserId() {
        HttpServletRequest request = currentRequest();
        Object userId = request.getAttribute(AuthConstants.ATTR_USER_ID);
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return (Long) userId;
    }

    public static String getSessionId() {
        HttpServletRequest request = currentRequest();
        Object sessionId = request.getAttribute(AuthConstants.ATTR_SESSION_ID);
        return sessionId == null ? null : String.valueOf(sessionId);
    }

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return attrs.getRequest();
    }
}
