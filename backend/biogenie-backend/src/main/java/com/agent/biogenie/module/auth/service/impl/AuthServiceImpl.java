package com.agent.biogenie.module.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agent.biogenie.config.SsoProperties;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.user.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.common.auth.AuthConstants;
import com.agent.biogenie.module.auth.service.AuthService;
import com.agent.biogenie.module.auth.service.UserSessionService;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.utils.JwtUtil;
import com.agent.biogenie.utils.LogUtils;
import com.agent.biogenie.utils.MinioUtil;
import com.agent.biogenie.utils.PasswordHashUtil;
import com.agent.biogenie.utils.SmsUtil;
import com.agent.biogenie.utils.WechatUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String SSO_TICKET_PREFIX = "sso:ticket:";
    private static final String SSO_ATTACHMENT_PREFIX = "sso:attachment:";
    private static final int MAX_SSO_ATTACHMENTS = 10;
    private static final long MAX_SSO_ATTACHMENT_SIZE = 200L * 1024 * 1024;
    private static final Set<String> ALLOWED_SSO_ATTACHMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/markdown", "text/x-markdown");

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final SmsUtil smsUtil;
    private final WechatUtil wechatUtil;
    private final UserSessionService userSessionService;
    private final SsoProperties ssoProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final MinioUtil minioUtil;

    @Override
    public SsoAttachmentResultDTO uploadSsoAttachment(String clientId, String clientSecret,
                                                       MultipartFile file) {
        SsoProperties.Client client = requireSsoClient(clientId, clientSecret);
        validateSsoAttachment(file);

        String attachmentId = IdUtil.fastSimpleUUID();
        String contentType = normalizeContentType(file.getContentType());
        String url = minioUtil.upload(file, "sso-staging/" + client.getSource() + "/");
        JSONObject payload = new JSONObject();
        payload.set("clientId", client.getClientId());
        payload.set("url", url);
        payload.set("name", StrUtil.blankToDefault(file.getOriginalFilename(), "attachment"));
        payload.set("contentType", contentType);
        payload.set("size", file.getSize());

        long ttl = Math.max(ssoProperties.getTicketTtlSeconds(), 1L);
        stringRedisTemplate.opsForValue().set(
                SSO_ATTACHMENT_PREFIX + attachmentId, payload.toString(), ttl, TimeUnit.SECONDS);
        return SsoAttachmentResultDTO.builder()
                .attachmentId(attachmentId)
                .name(payload.getStr("name"))
                .contentType(contentType)
                .size(file.getSize())
                .expireIn(ttl)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultDTO loginByPhone(LoginDTO loginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent) {
        boolean valid = smsUtil.verifyCode(loginDTO.getPhone(), loginDTO.getCode());
        if (!valid) {
            throw new BizException(ResultCode.SMS_CODE_ERROR);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, loginDTO.getPhone()));
        if (user == null) {
            user = new User();
            user.setPhone(loginDTO.getPhone());
            user.setNickname("手机用户" + loginDTO.getPhone().substring(7));
            user.setStatus(1);
            user.setRole(AuthConstants.ROLE_USER);
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(ip);
            userMapper.insert(user);
        } else {
            if (user.getStatus() == 0) {
                throw new BizException(ResultCode.USER_DISABLED);
            }
            updateLastLogin(user, ip);
        }

        return buildLoginResult(user, deviceInfo, ip, userAgent, Map.of("phone", user.getPhone()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultDTO loginByPassword(PasswordLoginDTO dto, String ip, DeviceInfoDTO deviceInfo, String userAgent) {
        String username = normalizeUsername(dto.getUsername());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        // Unknown accounts still perform the expensive hash to reduce username-enumeration timing signals.
        if (user == null) {
            PasswordHashUtil.hash(dto.getPassword().toCharArray(), "00000000-0000-4000-8000-000000000000");
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!PasswordHashUtil.matches(dto.getPassword().toCharArray(), user.getPasswordSalt(), user.getPasswordHash())) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        updateLastLogin(user, ip);
        return buildLoginResult(user, deviceInfo, ip, userAgent, Map.of("username", username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultDTO registerByPassword(PasswordRegisterDTO dto, String ip, DeviceInfoDTO deviceInfo,
                                               String userAgent) {
        String username = normalizeUsername(dto.getUsername());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.USER_EXISTED, "账号已存在");
        }

        String salt = PasswordHashUtil.newSalt();
        User user = new User();
        user.setUsername(username);
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordHashUtil.hash(dto.getPassword().toCharArray(), salt));
        user.setNickname(username);
        user.setStatus(1);
        user.setRole(AuthConstants.ROLE_USER);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.insert(user);
        return buildLoginResult(user, deviceInfo, ip, userAgent, Map.of("username", username));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultDTO loginByWechat(WechatLoginDTO wechatLoginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent) {
        wechatUtil.validateState(wechatLoginDTO.getState());

        JSONObject tokenInfo = wechatUtil.getAccessToken(wechatLoginDTO.getCode());
        String openid = tokenInfo.getStr("openid");
        String unionid = tokenInfo.getStr("unionid");
        String accessToken = tokenInfo.getStr("access_token");

        JSONObject wechatUserInfo = wechatUtil.getUserInfo(accessToken, openid);
        if (wechatUserInfo.isEmpty()) {
            LogUtils.logBusiness("WECHAT_USERINFO_WARN", "微信用户信息获取失败，将使用默认昵称/头像");
        }

        User user = resolveOrCreateWechatUser(openid, unionid, wechatUserInfo);

        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }

        updateLastLogin(user, ip);

        Map<String, Object> claims = new HashMap<>();
        claims.put("openid", user.getOpenid());
        claims.put("phone", StrUtil.nullToEmpty(user.getPhone()));
        return buildLoginResult(user, deviceInfo, ip, userAgent, claims);
    }

    @Override
    public SsoTicketResultDTO createSsoTicket(SsoTicketRequestDTO request) {
        SsoProperties.Client client = requireSsoClient(request.getClientId(), request.getClientSecret());

        String externalUserId = StrUtil.blankToDefault(request.getExternalUserId(), IdUtil.fastSimpleUUID());
        String ticket = IdUtil.fastSimpleUUID();
        JSONObject payload = new JSONObject();
        payload.set("externalUserId", externalUserId);
        payload.set("source", client.getSource());
        String targetModule = StrUtil.trim(request.getTargetModule());
        String question = StrUtil.trim(request.getQuestion());
        List<String> attachmentIds = request.getAttachmentIds() == null
                ? List.of() : request.getAttachmentIds();
        if (attachmentIds.size() > MAX_SSO_ATTACHMENTS) {
            throw new BizException(ResultCode.BAD_REQUEST, "附件数量不能超过 10");
        }
        boolean hasQuestion = StrUtil.isNotBlank(question);
        if (hasQuestion && StrUtil.isBlank(targetModule)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "传入 question 时必须同时传入 targetModule");
        }
        if (!attachmentIds.isEmpty() && !hasQuestion) {
            throw new BizException(ResultCode.BAD_REQUEST, "attachmentIds 不能脱离 question 单独使用");
        }
        if (StrUtil.isNotBlank(targetModule)) {
            payload.set("targetModule", targetModule);
            if (StrUtil.isNotBlank(question)) {
                payload.set("question", question);
            }
            if (!attachmentIds.isEmpty()) {
                payload.set("attachments", resolveSsoAttachments(client.getClientId(), attachmentIds));
            }
        }

        long ttl = Math.max(ssoProperties.getTicketTtlSeconds(), 1L);
        stringRedisTemplate.opsForValue().set(
                SSO_TICKET_PREFIX + ticket,
                payload.toString(),
                ttl,
                TimeUnit.SECONDS);
        attachmentIds.forEach(id -> stringRedisTemplate.delete(SSO_ATTACHMENT_PREFIX + id));

        LogUtils.logBusiness("SSO_TICKET_CREATED",
                "SSO ticket 已签发 | clientId=%s | source=%s | ttl=%s",
                request.getClientId(), client.getSource(), ttl);

        return SsoTicketResultDTO.builder()
                .ticket(ticket)
                .externalUserId(externalUserId)
                .expireIn(ttl)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultDTO loginBySso(SsoLoginDTO ssoLoginDTO, String ip, DeviceInfoDTO deviceInfo, String userAgent) {
        String redisKey = SSO_TICKET_PREFIX + ssoLoginDTO.getTicket();
        String payloadJson = stringRedisTemplate.opsForValue().getAndDelete(redisKey);
        if (StrUtil.isBlank(payloadJson)) {
            throw new BizException(ResultCode.SSO_TICKET_INVALID);
        }

        JSONObject payload = JSONUtil.parseObj(payloadJson);
        String externalUserId = payload.getStr("externalUserId");
        String source = payload.getStr("source");
        if (StrUtil.isBlank(externalUserId) || StrUtil.isBlank(source)) {
            throw new BizException(ResultCode.SSO_TICKET_INVALID);
        }

        User user = resolveOrCreateSsoUser(source, externalUserId);

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }

        updateLastLogin(user, ip);

        Map<String, Object> claims = new HashMap<>();
        claims.put("source", source);
        claims.put("externalId", externalUserId);
        claims.put("phone", StrUtil.nullToEmpty(user.getPhone()));
        LoginResultDTO result = buildLoginResult(user, deviceInfo, ip, userAgent, claims);
        result.setTargetModule(payload.getStr("targetModule"));
        result.setQuestion(payload.getStr("question"));
        if (payload.getJSONArray("attachments") != null) {
            result.setAttachments(JSONUtil.toList(payload.getJSONArray("attachments"), ChatAttachmentDTO.class));
        }
        return result;
    }

    private SsoProperties.Client requireSsoClient(String clientId, String clientSecret) {
        SsoProperties.Client client = ssoProperties.findClient(clientId);
        if (client == null || StrUtil.isBlank(client.getClientSecret())
                || StrUtil.isBlank(client.getSource())
                || !secureEquals(client.getClientSecret(), clientSecret)) {
            throw new BizException(ResultCode.SSO_CLIENT_INVALID);
        }
        return client;
    }

    private List<ChatAttachmentDTO> resolveSsoAttachments(String clientId, List<String> attachmentIds) {
        List<ChatAttachmentDTO> attachments = new ArrayList<>();
        for (String attachmentId : attachmentIds) {
            String raw = stringRedisTemplate.opsForValue().get(SSO_ATTACHMENT_PREFIX + attachmentId);
            if (StrUtil.isBlank(raw)) {
                throw new BizException(ResultCode.BAD_REQUEST, "附件不存在或已过期: " + attachmentId);
            }
            JSONObject stored = JSONUtil.parseObj(raw);
            if (!secureEquals(clientId, stored.getStr("clientId"))) {
                throw new BizException(ResultCode.BAD_REQUEST, "附件不属于当前 SSO 客户端");
            }
            attachments.add(ChatAttachmentDTO.builder()
                    .url(stored.getStr("url"))
                    .name(stored.getStr("name"))
                    .contentType(stored.getStr("contentType"))
                    .size(stored.getLong("size"))
                    .build());
        }
        return attachments;
    }

    private void validateSsoAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "附件不能为空");
        }
        if (file.getSize() > MAX_SSO_ATTACHMENT_SIZE) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED, "附件大小不能超过 200MB");
        }
        if (!ALLOWED_SSO_ATTACHMENT_TYPES.contains(normalizeContentType(file.getContentType()))) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) return "";
        int separator = contentType.indexOf(';');
        return (separator >= 0 ? contentType.substring(0, separator) : contentType)
                .trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public WechatQrConfigDTO getWechatQrConfig() {
        return wechatUtil.buildQrConfig();
    }

    @Override
    public void sendSmsCode(String phone) {
        smsUtil.sendCode(phone);
    }

    @Override
    public List<UserSessionDTO> listSessions(Long userId, String currentSessionId) {
        return userSessionService.listSessions(userId, currentSessionId);
    }

    @Override
    public int countSessions(Long userId) {
        return userSessionService.countSessions(userId);
    }

    @Override
    public void logout(Long userId, String sessionId) {
        userSessionService.removeSession(userId, sessionId);
    }

    @Override
    public void kickSession(Long userId, String currentSessionId, String targetSessionId) {
        if (StrUtil.isBlank(targetSessionId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "会话ID不能为空");
        }
        if (!userSessionService.validateSession(userId, targetSessionId)) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在或已失效");
        }
        userSessionService.removeSession(userId, targetSessionId);
        LogUtils.logUserOperation("KICK_SESSION", "session", "SUCCESS");
    }

    @Override
    public TokenRefreshResultDTO refreshAccessToken(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        if (!jwtUtil.isRefreshToken(claims)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "无效的 Refresh Token");
        }

        Long userId = jwtUtil.getUserId(claims);
        String sessionId = jwtUtil.getSessionId(claims);
        if (StrUtil.isBlank(sessionId)) {
            throw new BizException(ResultCode.SESSION_INVALID);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            userSessionService.removeAllSessions(userId);
            throw new BizException(ResultCode.USER_DISABLED);
        }

        if (!userSessionService.validateSession(userId, sessionId)) {
            throw new BizException(ResultCode.SESSION_INVALID);
        }

        userSessionService.touchSession(userId, sessionId);

        Map<String, Object> accessClaims = jwtUtil.copyBusinessClaims(claims);
        accessClaims.put(JwtUtil.CLAIM_SID, sessionId);
        String accessToken = jwtUtil.generateAccessToken(userId, accessClaims);

        return TokenRefreshResultDTO.builder()
                .token(accessToken)
                .expireTime(jwtUtil.getExpireTimeMillis())
                .build();
    }

    /**
     * 按 openid 查找，未找到则按 unionid 关联已有账号，否则创建新用户
     */
    private User resolveOrCreateWechatUser(String openid, String unionid, JSONObject wechatUserInfo) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));

        if (user != null) {
            applyWechatProfile(user, unionid, wechatUserInfo);
            return user;
        }

        if (StrUtil.isNotBlank(unionid)) {
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUnionid, unionid));
            if (user != null) {
                if (StrUtil.isNotBlank(user.getOpenid()) && !openid.equals(user.getOpenid())) {
                    log.warn("UnionID 账户 openid 冲突");
                    LogUtils.logBusiness("WECHAT_OPENID_CONFLICT", "UnionID账户openid冲突");
                } else {
                    user.setOpenid(openid);
                }
                applyWechatProfile(user, unionid, wechatUserInfo);
                return user;
            }
        }

        user = new User();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        user.setNickname(wechatUserInfo.getStr("nickname", "微信用户"));
        user.setAvatar(wechatUserInfo.getStr("headimgurl", ""));
        user.setGender(wechatUserInfo.getInt("sex", 0));
        user.setStatus(1);
        user.setRole(AuthConstants.ROLE_USER);
        userMapper.insert(user);
        return user;
    }

    /**
     * 按 source+externalId 查找，未找到则创建外部游客用户
     */
    private User resolveOrCreateSsoUser(String source, String externalUserId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getSource, source)
                .eq(User::getExternalId, externalUserId));
        if (user != null) {
            return user;
        }

        user = new User();
        user.setSource(source);
        user.setExternalId(externalUserId);
        user.setNickname("外部用户" + externalUserId.substring(0, Math.min(8, externalUserId.length())));
        user.setAvatar("");
        user.setGender(0);
        user.setStatus(1);
        user.setRole(AuthConstants.ROLE_USER);
        userMapper.insert(user);
        return user;
    }

    private void applyWechatProfile(User user, String unionid, JSONObject wechatUserInfo) {
        if (StrUtil.isNotBlank(unionid)) {
            user.setUnionid(unionid);
        }
        if (wechatUserInfo.containsKey("nickname")) {
            user.setNickname(wechatUserInfo.getStr("nickname"));
        }
        if (wechatUserInfo.containsKey("headimgurl")) {
            user.setAvatar(wechatUserInfo.getStr("headimgurl"));
        }
    }

    private void updateLastLogin(User user, String ip) {
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);
    }

    private LoginResultDTO buildLoginResult(User user, DeviceInfoDTO deviceInfo, String ip, String userAgent,
                                            Map<String, Object> extraClaims) {
        String sessionId = userSessionService.createSession(user.getId(), deviceInfo, ip, userAgent);

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(JwtUtil.CLAIM_SID, sessionId);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(),
                Map.of(JwtUtil.CLAIM_SID, sessionId));

        return LoginResultDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .sessionId(sessionId)
                .deviceCount(userSessionService.countSessions(user.getId()))
                .expireTime(jwtUtil.getExpireTimeMillis())
                .refreshExpireTime(jwtUtil.getRefreshExpireTimeMillis())
                .userInfo(UserInfoDTO.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(StrUtil.isBlank(user.getAvatar()) ? user.getAvatar() : minioUtil.toAccessUrl(user.getAvatar()))
                        .phone(maskPhone(user.getPhone()))
                        .gender(user.getGender())
                        .role(StrUtil.blankToDefault(user.getRole(), AuthConstants.ROLE_USER))
                        .build())
                .build();
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
