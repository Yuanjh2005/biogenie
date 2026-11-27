package com.agent.biogenie.module.auth.service.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.SsoProperties;
import com.agent.biogenie.dto.user.DeviceInfoDTO;
import com.agent.biogenie.dto.user.LoginResultDTO;
import com.agent.biogenie.dto.user.SsoLoginDTO;
import com.agent.biogenie.dto.user.SsoTicketRequestDTO;
import com.agent.biogenie.dto.user.SsoTicketResultDTO;
import com.agent.biogenie.dto.user.WechatLoginDTO;
import com.agent.biogenie.module.auth.service.UserSessionService;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.utils.JwtUtil;
import com.agent.biogenie.utils.MinioUtil;
import com.agent.biogenie.utils.SmsUtil;
import com.agent.biogenie.utils.WechatUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SmsUtil smsUtil;

    @Mock
    private WechatUtil wechatUtil;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private SsoProperties ssoProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final long EXPIRATION = 3_600_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;
    private static final DeviceInfoDTO DEVICE_INFO = DeviceInfoDTO.builder()
            .deviceType("Web")
            .deviceName("Chrome on Windows")
            .build();
    private static final String USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        lenient().when(jwtUtil.getExpireTimeMillis()).thenReturn(System.currentTimeMillis() + EXPIRATION);
        lenient().when(jwtUtil.getRefreshExpireTimeMillis()).thenReturn(System.currentTimeMillis() + REFRESH_EXPIRATION);
        lenient().when(jwtUtil.generateAccessToken(anyLong(), anyMap())).thenReturn("test-access-token");
        lenient().when(jwtUtil.generateRefreshToken(anyLong(), anyMap())).thenReturn("test-refresh-token");
        lenient().when(userSessionService.createSession(anyLong(), any(), anyString(), anyString())).thenReturn("session-123");
        lenient().when(userSessionService.countSessions(anyLong())).thenReturn(2);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void loginByWechat_shouldMergeByUnionidWhenOpenidNotFound() {
        WechatLoginDTO dto = new WechatLoginDTO();
        dto.setCode("code123");
        dto.setState("state123");

        JSONObject tokenInfo = new JSONObject();
        tokenInfo.set("openid", "new-openid");
        tokenInfo.set("unionid", "union-123");
        tokenInfo.set("access_token", "access-token");

        JSONObject userInfo = new JSONObject();
        userInfo.set("nickname", "微信昵称");
        userInfo.set("headimgurl", "http://avatar.url");
        userInfo.set("sex", 1);

        User existingUser = new User();
        existingUser.setId(10L);
        existingUser.setUnionid("union-123");
        existingUser.setPhone("13800138000");
        existingUser.setStatus(1);

        when(wechatUtil.getAccessToken("code123")).thenReturn(tokenInfo);
        when(wechatUtil.getUserInfo("access-token", "new-openid")).thenReturn(userInfo);
        doNothing().when(wechatUtil).validateState("state123");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null)
                .thenReturn(existingUser);

        LoginResultDTO result = authService.loginByWechat(dto, "127.0.0.1", DEVICE_INFO, USER_AGENT);

        assertEquals("test-access-token", result.getToken());
        assertEquals("test-refresh-token", result.getRefreshToken());
        assertEquals("session-123", result.getSessionId());
        assertEquals(2, result.getDeviceCount());
        assertEquals(EXPIRATION, result.getExpireTime() - System.currentTimeMillis(), 2000);
        assertEquals(REFRESH_EXPIRATION, result.getRefreshExpireTime() - System.currentTimeMillis(), 2000);
        assertEquals("new-openid", existingUser.getOpenid());
        verify(userMapper).updateById(existingUser);
        verify(userSessionService).createSession(eq(10L), eq(DEVICE_INFO), eq("127.0.0.1"), eq(USER_AGENT));
        verify(jwtUtil).generateAccessToken(eq(10L), argThat(claims ->
                "session-123".equals(claims.get("sid")) && "new-openid".equals(claims.get("openid"))));
        verify(jwtUtil).generateRefreshToken(eq(10L), argThat(claims ->
                "session-123".equals(claims.get("sid"))));
    }

    @Test
    void loginByWechat_shouldCreateNewUserWhenNotFound() {
        WechatLoginDTO dto = new WechatLoginDTO();
        dto.setCode("code456");
        dto.setState("state456");

        JSONObject tokenInfo = new JSONObject();
        tokenInfo.set("openid", "openid-new");
        tokenInfo.set("unionid", "union-new");
        tokenInfo.set("access_token", "access-token");

        JSONObject userInfo = new JSONObject();
        userInfo.set("nickname", "新用户");
        userInfo.set("headimgurl", "http://avatar.url");
        userInfo.set("sex", 0);

        when(wechatUtil.getAccessToken("code456")).thenReturn(tokenInfo);
        when(wechatUtil.getUserInfo("access-token", "openid-new")).thenReturn(userInfo);
        doNothing().when(wechatUtil).validateState("state456");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        });

        LoginResultDTO result = authService.loginByWechat(dto, "127.0.0.1", DEVICE_INFO, USER_AGENT);

        assertEquals("test-access-token", result.getToken());
        assertEquals("test-refresh-token", result.getRefreshToken());
        assertEquals("session-123", result.getSessionId());
        assertEquals(99L, result.getUserInfo().getId());
        verify(userMapper).insert(any(User.class));
        verify(userMapper).updateById(any(User.class));
        verify(userSessionService).createSession(eq(99L), eq(DEVICE_INFO), eq("127.0.0.1"), eq(USER_AGENT));
    }

    @Test
    void refreshAccessToken_shouldReturnNewAccessToken() {
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        User user = new User();
        user.setId(10L);
        user.setStatus(1);

        when(jwtUtil.parseToken("refresh-token")).thenReturn(claims);
        when(jwtUtil.isRefreshToken(claims)).thenReturn(true);
        when(jwtUtil.getUserId(claims)).thenReturn(10L);
        when(jwtUtil.getSessionId(claims)).thenReturn("session-123");
        when(userMapper.selectById(10L)).thenReturn(user);
        when(userSessionService.validateSession(10L, "session-123")).thenReturn(true);
        when(jwtUtil.copyBusinessClaims(claims)).thenReturn(new java.util.HashMap<>(Map.of("sid", "session-123")));
        when(jwtUtil.generateAccessToken(eq(10L), anyMap())).thenReturn("new-access-token");
        when(jwtUtil.getExpireTimeMillis()).thenReturn(System.currentTimeMillis() + EXPIRATION);

        var result = authService.refreshAccessToken("refresh-token");

        assertEquals("new-access-token", result.getToken());
        verify(userSessionService).touchSession(10L, "session-123");
    }

    @Test
    void createSsoTicket_shouldStoreTicketInRedis() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");

        when(ssoProperties.findClient("partner-a")).thenReturn(client);
        when(ssoProperties.getTicketTtlSeconds()).thenReturn(1800L);

        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("secret-a");
        request.setExternalUserId("ext-1");
        request.setTargetModule("smart-drgf");
        request.setQuestion("EGFR 与肺癌有什么关系？");

        SsoTicketResultDTO result = authService.createSsoTicket(request);

        assertNotNull(result.getTicket());
        assertEquals("ext-1", result.getExternalUserId());
        assertEquals(1800L, result.getExpireIn());
        verify(valueOperations).set(startsWith("sso:ticket:"),
                argThat(payload -> payload.contains("\"targetModule\":\"smart-drgf\"")
                        && payload.contains("\"question\":\"EGFR 与肺癌有什么关系？\"")),
                eq(1800L), eq(TimeUnit.SECONDS));
    }

    @Test
    void uploadAndBindSsoAttachment_shouldStoreMetadataAndConsumeAttachmentId() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");
        when(ssoProperties.findClient("partner-a")).thenReturn(client);
        when(ssoProperties.getTicketTtlSeconds()).thenReturn(1800L);
        when(minioUtil.upload(any(), eq("sso-staging/PARTNER_A/")))
                .thenReturn("bucket/sso-staging/PARTNER_A/report.pdf");

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "pdf".getBytes());
        var upload = authService.uploadSsoAttachment("partner-a", "secret-a", file);

        assertNotNull(upload.getAttachmentId());
        verify(valueOperations).set(eq("sso:attachment:" + upload.getAttachmentId()),
                contains("report.pdf"), eq(1800L), eq(TimeUnit.SECONDS));

        when(valueOperations.get("sso:attachment:" + upload.getAttachmentId())).thenReturn(
                "{\"clientId\":\"partner-a\",\"url\":\"bucket/report.pdf\"," +
                        "\"name\":\"report.pdf\",\"contentType\":\"application/pdf\",\"size\":3}");
        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("secret-a");
        request.setTargetModule("smart-drgf");
        request.setQuestion("请分析附件中的实验结果");
        request.setAttachmentIds(List.of(upload.getAttachmentId()));

        authService.createSsoTicket(request);

        verify(valueOperations).set(startsWith("sso:ticket:"), contains("attachments"),
                eq(1800L), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate).delete("sso:attachment:" + upload.getAttachmentId());
    }

    @Test
    void createSsoTicket_shouldRejectAttachmentsWithoutQuestion() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");
        when(ssoProperties.findClient("partner-a")).thenReturn(client);

        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("secret-a");
        request.setTargetModule("smart-drgf");
        request.setAttachmentIds(List.of("attachment-1"));

        BizException ex = assertThrows(BizException.class, () -> authService.createSsoTicket(request));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(valueOperations, never()).get(anyString());
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void createSsoTicket_shouldGenerateExternalUserIdWhenMissing() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");

        when(ssoProperties.findClient("partner-a")).thenReturn(client);
        when(ssoProperties.getTicketTtlSeconds()).thenReturn(1800L);

        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("secret-a");

        SsoTicketResultDTO result = authService.createSsoTicket(request);

        assertNotNull(result.getTicket());
        assertNotNull(result.getExternalUserId());
        assertFalse(result.getExternalUserId().isBlank());
        verify(valueOperations).set(startsWith("sso:ticket:"), contains(result.getExternalUserId()),
                eq(1800L), eq(TimeUnit.SECONDS));
    }

    @Test
    void createSsoTicket_shouldRejectInvalidSecret() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");
        when(ssoProperties.findClient("partner-a")).thenReturn(client);

        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("wrong");

        BizException ex = assertThrows(BizException.class, () -> authService.createSsoTicket(request));
        assertEquals(ResultCode.SSO_CLIENT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createSsoTicket_shouldAllowTargetModuleWithoutQuestionAndAttachments() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");
        when(ssoProperties.findClient("partner-a")).thenReturn(client);
        when(ssoProperties.getTicketTtlSeconds()).thenReturn(1800L);

        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("secret-a");
        request.setTargetModule("smart-drgf");

        SsoTicketResultDTO result = authService.createSsoTicket(request);

        assertNotNull(result.getTicket());
        verify(valueOperations).set(startsWith("sso:ticket:"),
                argThat(payload -> payload.contains("\"targetModule\":\"smart-drgf\"")
                        && !payload.contains("\"question\"")
                        && !payload.contains("\"attachments\"")),
                eq(1800L), eq(TimeUnit.SECONDS));
    }

    @Test
    void createSsoTicket_shouldRejectQuestionWithoutTargetModule() {
        SsoProperties.Client client = new SsoProperties.Client();
        client.setClientId("partner-a");
        client.setClientSecret("secret-a");
        client.setSource("PARTNER_A");
        when(ssoProperties.findClient("partner-a")).thenReturn(client);

        SsoTicketRequestDTO request = new SsoTicketRequestDTO();
        request.setClientId("partner-a");
        request.setClientSecret("secret-a");
        request.setQuestion("什么是 EGFR？");

        BizException ex = assertThrows(BizException.class, () -> authService.createSsoTicket(request));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void loginBySso_shouldCreateUserAndIssueToken() {
        String payload = "{\"externalUserId\":\"ext-9\",\"source\":\"PARTNER_A\","
                + "\"targetModule\":\"smart-drgf\",\"question\":\"什么是 EGFR？\"}";
        when(valueOperations.getAndDelete("sso:ticket:ticket-abc")).thenReturn(payload);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(88L);
            return 1;
        });

        SsoLoginDTO dto = new SsoLoginDTO();
        dto.setTicket("ticket-abc");

        LoginResultDTO result = authService.loginBySso(dto, "127.0.0.1", DEVICE_INFO, USER_AGENT);

        assertEquals("test-access-token", result.getToken());
        assertEquals(88L, result.getUserInfo().getId());
        assertEquals("smart-drgf", result.getTargetModule());
        assertEquals("什么是 EGFR？", result.getQuestion());
        verify(userMapper).insert(any(User.class));
        verify(userSessionService).createSession(eq(88L), eq(DEVICE_INFO), eq("127.0.0.1"), eq(USER_AGENT));
    }

    @Test
    void loginBySso_shouldRejectMissingTicket() {
        when(valueOperations.getAndDelete("sso:ticket:missing")).thenReturn(null);

        SsoLoginDTO dto = new SsoLoginDTO();
        dto.setTicket("missing");

        BizException ex = assertThrows(BizException.class,
                () -> authService.loginBySso(dto, "127.0.0.1", DEVICE_INFO, USER_AGENT));
        assertEquals(ResultCode.SSO_TICKET_INVALID.getCode(), ex.getCode());
    }
}
