package com.agent.biogenie.module.auth.service;

import com.agent.biogenie.dto.user.DeviceInfoDTO;
import com.agent.biogenie.dto.user.LoginDTO;
import com.agent.biogenie.dto.user.LoginResultDTO;
import com.agent.biogenie.dto.user.PasswordLoginDTO;
import com.agent.biogenie.dto.user.PasswordRegisterDTO;
import com.agent.biogenie.dto.user.SsoLoginDTO;
import com.agent.biogenie.dto.user.SsoTicketRequestDTO;
import com.agent.biogenie.dto.user.SsoTicketResultDTO;
import com.agent.biogenie.dto.user.SsoAttachmentResultDTO;
import com.agent.biogenie.dto.user.TokenRefreshResultDTO;
import com.agent.biogenie.dto.user.UserSessionDTO;
import com.agent.biogenie.dto.user.WechatLoginDTO;
import com.agent.biogenie.dto.user.WechatQrConfigDTO;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 手机号 + 验证码登录
     */
    LoginResultDTO loginByPhone(LoginDTO loginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent);

    LoginResultDTO loginByPassword(PasswordLoginDTO loginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent);

    LoginResultDTO registerByPassword(PasswordRegisterDTO registerDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent);

    /**
     * 微信开放平台 PC 端扫码登录
     */
    LoginResultDTO loginByWechat(WechatLoginDTO wechatLoginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent);

    /**
     * 外部系统换取一次性 SSO ticket（服务端调用）
     */
    SsoTicketResultDTO createSsoTicket(SsoTicketRequestDTO request);

    /** 外部系统在换票前暂存一个启动附件。 */
    SsoAttachmentResultDTO uploadSsoAttachment(String clientId, String clientSecret, MultipartFile file);

    /**
     * SSO ticket 免登登录（前端调用）
     */
    LoginResultDTO loginBySso(SsoLoginDTO ssoLoginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent);

    /**
     * 获取微信 PC 端扫码登录配置
     */
    WechatQrConfigDTO getWechatQrConfig();

    /**
     * 发送短信验证码
     */
    void sendSmsCode(String phone);

    /**
     * 查询当前用户所有在线会话
     */
    List<UserSessionDTO> listSessions(Long userId, String currentSessionId);

    /**
     * 查询当前用户在线台数
     */
    int countSessions(Long userId);

    /**
     * 当前设备退出登录
     */
    void logout(Long userId, String sessionId);

    /**
     * 踢掉指定设备会话
     */
    void kickSession(Long userId, String currentSessionId, String targetSessionId);

    /**
     * 使用 Refresh Token 换取新的 Access Token
     */
    TokenRefreshResultDTO refreshAccessToken(String refreshToken);
}
