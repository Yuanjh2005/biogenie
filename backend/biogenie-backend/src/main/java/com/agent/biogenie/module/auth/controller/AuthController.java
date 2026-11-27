package com.agent.biogenie.module.auth.controller;

import com.agent.biogenie.common.Result;
import com.agent.biogenie.common.auth.Anonymous;
import com.agent.biogenie.common.auth.AuthContext;
import com.agent.biogenie.common.auth.AuthConstants;
import com.agent.biogenie.dto.user.DeviceInfoDTO;
import com.agent.biogenie.dto.user.LoginDTO;
import com.agent.biogenie.dto.user.LoginResultDTO;
import com.agent.biogenie.dto.user.PasswordLoginDTO;
import com.agent.biogenie.dto.user.PasswordRegisterDTO;
import com.agent.biogenie.dto.user.RefreshTokenDTO;
import com.agent.biogenie.dto.user.SmsCodeDTO;
import com.agent.biogenie.dto.user.SsoLoginDTO;
import com.agent.biogenie.dto.user.SsoAttachmentResultDTO;
import com.agent.biogenie.dto.user.SsoTicketRequestDTO;
import com.agent.biogenie.dto.user.SsoTicketResultDTO;
import com.agent.biogenie.dto.user.TokenRefreshResultDTO;
import com.agent.biogenie.dto.user.UserSessionDTO;
import com.agent.biogenie.dto.user.WechatLoginDTO;
import com.agent.biogenie.dto.user.WechatQrConfigDTO;
import com.agent.biogenie.module.auth.service.AuthService;
import com.agent.biogenie.utils.DeviceInfoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "登录、短信验证码、会话管理相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Anonymous
    @Operation(summary = "手机号+验证码登录")
    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = getClientIp(request);
        DeviceInfoDTO deviceInfo = DeviceInfoUtil.resolve(request);
        String userAgent = DeviceInfoUtil.getUserAgent(request);
        LoginResultDTO result = authService.loginByPhone(loginDTO, ip, deviceInfo, userAgent);
        markAuthenticated(request, result);
        return Result.ok("登录成功", result);
    }

    @Anonymous
    @Operation(summary = "账号密码登录")
    @PostMapping("/login/password")
    public Result<LoginResultDTO> passwordLogin(@Valid @RequestBody PasswordLoginDTO dto,
                                                 HttpServletRequest request) {
        LoginResultDTO result = authService.loginByPassword(dto, getClientIp(request),
                DeviceInfoUtil.resolve(request), DeviceInfoUtil.getUserAgent(request));
        markAuthenticated(request, result);
        return Result.ok("登录成功", result);
    }

    @Anonymous
    @Operation(summary = "注册账号并登录")
    @PostMapping("/register/password")
    public Result<LoginResultDTO> passwordRegister(@Valid @RequestBody PasswordRegisterDTO dto,
                                                    HttpServletRequest request) {
        LoginResultDTO result = authService.registerByPassword(dto, getClientIp(request),
                DeviceInfoUtil.resolve(request), DeviceInfoUtil.getUserAgent(request));
        markAuthenticated(request, result);
        return Result.ok("注册成功", result);
    }

    @Anonymous
    @Operation(summary = "微信开放平台 PC 端扫码登录")
    @PostMapping("/login/wechat")
    public Result<LoginResultDTO> wechatLogin(@Valid @RequestBody WechatLoginDTO wechatLoginDTO,
                                               HttpServletRequest request) {
        String ip = getClientIp(request);
        DeviceInfoDTO deviceInfo = DeviceInfoUtil.resolve(request);
        String userAgent = DeviceInfoUtil.getUserAgent(request);
        LoginResultDTO result = authService.loginByWechat(wechatLoginDTO, ip, deviceInfo, userAgent);
        markAuthenticated(request, result);
        return Result.ok("微信登录成功", result);
    }

    @Anonymous
    @Operation(summary = "外部系统换取 SSO ticket（服务端调用）")
    @PostMapping("/sso/ticket")
    public Result<SsoTicketResultDTO> createSsoTicket(@Valid @RequestBody SsoTicketRequestDTO request) {
        return Result.ok(authService.createSsoTicket(request));
    }

    @Anonymous
    @Operation(summary = "外部系统暂存 SSO 启动附件（服务端调用）")
    @PostMapping(value = "/sso/attachment", consumes = "multipart/form-data")
    public Result<SsoAttachmentResultDTO> uploadSsoAttachment(
            @RequestParam String clientId,
            @RequestParam String clientSecret,
            @RequestParam("file") MultipartFile file) {
        return Result.ok(authService.uploadSsoAttachment(clientId, clientSecret, file));
    }

    @Anonymous
    @Operation(summary = "SSO ticket 免登登录")
    @PostMapping("/login/sso")
    public Result<LoginResultDTO> ssoLogin(@Valid @RequestBody SsoLoginDTO ssoLoginDTO,
                                           HttpServletRequest request) {
        String ip = getClientIp(request);
        DeviceInfoDTO deviceInfo = DeviceInfoUtil.resolve(request);
        String userAgent = DeviceInfoUtil.getUserAgent(request);
        LoginResultDTO result = authService.loginBySso(ssoLoginDTO, ip, deviceInfo, userAgent);
        markAuthenticated(request, result);
        return Result.ok("登录成功", result);
    }

    @Anonymous
    @Operation(summary = "获取微信 PC 端扫码登录配置")
    @GetMapping("/wechat/qrcode")
    public Result<WechatQrConfigDTO> wechatQrConfig() {
        WechatQrConfigDTO config = authService.getWechatQrConfig();
        return Result.ok(config);
    }

    @Anonymous
    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-code")
    public Result<String> sendSmsCode(@Valid @RequestBody SmsCodeDTO smsCodeDTO) {
        authService.sendSmsCode(smsCodeDTO.getPhone());
        return Result.ok("验证码发送成功");
    }

    @Anonymous
    @Operation(summary = "刷新 Access Token")
    @PostMapping("/refresh")
    public Result<TokenRefreshResultDTO> refresh(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        TokenRefreshResultDTO result = authService.refreshAccessToken(refreshTokenDTO.getRefreshToken());
        return Result.ok("刷新成功", result);
    }

    @Operation(summary = "查询当前用户所有在线设备")
    @GetMapping("/sessions")
    public Result<List<UserSessionDTO>> listSessions() {
        return Result.ok(authService.listSessions(AuthContext.getUserId(), AuthContext.getSessionId()));
    }

    @Operation(summary = "查询当前用户在线台数")
    @GetMapping("/sessions/count")
    public Result<Integer> countSessions() {
        return Result.ok(authService.countSessions(AuthContext.getUserId()));
    }

    @Operation(summary = "当前设备退出登录")
    @PostMapping("/logout")
    public Result<String> logout() {
        authService.logout(AuthContext.getUserId(), AuthContext.getSessionId());
        return Result.ok("退出成功");
    }

    @Operation(summary = "踢掉指定设备会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<String> kickSession(@PathVariable String sessionId) {
        authService.kickSession(AuthContext.getUserId(), AuthContext.getSessionId(), sessionId);
        return Result.ok("设备已下线");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void markAuthenticated(HttpServletRequest request, LoginResultDTO result) {
        if (result != null && result.getUserInfo() != null) {
            request.setAttribute(AuthConstants.ATTR_USER_ID, result.getUserInfo().getId());
            request.setAttribute(AuthConstants.ATTR_ROLE,
                    result.getUserInfo().getRole() == null ? AuthConstants.ROLE_USER : result.getUserInfo().getRole());
        }
    }
}
