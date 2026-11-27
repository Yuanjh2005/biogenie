package com.agent.biogenie.module.auth.service;

import com.agent.biogenie.dto.user.DeviceInfoDTO;
import com.agent.biogenie.dto.user.UserSessionDTO;

import java.util.List;

/**
 * 用户多端登录会话服务
 */
public interface UserSessionService {

    /**
     * 创建登录会话
     */
    String createSession(Long userId, DeviceInfoDTO deviceInfo, String clientIp, String userAgent);

    /**
     * 校验会话是否有效
     */
    boolean validateSession(Long userId, String sessionId);

    /**
     * 更新最后活跃时间（内部节流）
     */
    void touchSession(Long userId, String sessionId);

    /**
     * 查询用户所有在线会话
     */
    List<UserSessionDTO> listSessions(Long userId, String currentSessionId);

    /**
     * 统计用户当前在线台数
     */
    int countSessions(Long userId);

    /**
     * 删除指定会话（踢下线 / 退出登录）
     */
    void removeSession(Long userId, String sessionId);

    /**
     * 删除用户全部会话（如禁用账号时踢下线）
     */
    void removeAllSessions(Long userId);
}
