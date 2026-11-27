package com.agent.biogenie.utils;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.WechatProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatUtilTest {

    @Mock
    private WechatProperties wechatProperties;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private WechatUtil wechatUtil;

    @Test
    void validateState_whenBlank_shouldThrowBadRequest() {
        BizException ex = assertThrows(BizException.class, () -> wechatUtil.validateState(""));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void validateState_whenExpired_shouldThrowWechatCodeInvalid() {
        when(redisTemplate.hasKey("wechat:state:abc123")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> wechatUtil.validateState("abc123"));
        assertEquals(ResultCode.WECHAT_CODE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void validateState_whenValid_shouldDeleteKey() {
        when(redisTemplate.hasKey("wechat:state:valid-state")).thenReturn(true);

        assertDoesNotThrow(() -> wechatUtil.validateState("valid-state"));
        verify(redisTemplate).delete("wechat:state:valid-state");
    }

    @Test
    void buildQrConfig_whenConfigIncomplete_shouldThrow() {
        when(wechatProperties.getAppId()).thenReturn("");
        when(wechatProperties.getAppSecret()).thenReturn("");
        when(wechatProperties.getRedirectUri()).thenReturn("");

        BizException ex = assertThrows(BizException.class, () -> wechatUtil.buildQrConfig());
        assertEquals(ResultCode.WECHAT_LOGIN_FAILED.getCode(), ex.getCode());
    }

    @Test
    void buildQrConfig_whenConfigValid_shouldReturnQrConnectUrl() {
        when(wechatProperties.getAppId()).thenReturn("wx-app-id");
        when(wechatProperties.getAppSecret()).thenReturn("secret");
        when(wechatProperties.getRedirectUri()).thenReturn("http://localhost:3000/callback");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        var config = wechatUtil.buildQrConfig();

        assertEquals("wx-app-id", config.getAppId());
        assertEquals("snsapi_login", config.getScope());
        assertNotNull(config.getState());
        assertTrue(config.getQrConnectUrl().contains("open.weixin.qq.com/connect/qrconnect"));
        verify(valueOperations).set(startsWith("wechat:state:"), eq("1"), eq(5L), any());
    }
}
