package com.agent.biogenie.utils;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.SmsProperties;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsUtilTest {

    @Mock
    private SmsProperties smsProperties;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private AsyncClient dypnsAsyncClient;

    @InjectMocks
    private SmsUtil smsUtil;

    @BeforeEach
    void setUp() {
        lenient().when(smsProperties.getAccessKeyId()).thenReturn("LTAI_REAL_KEY");
        lenient().when(smsProperties.getAccessKeySecret()).thenReturn("real-secret");
        lenient().when(smsProperties.getSignName()).thenReturn("速通互联验证码");
        lenient().when(smsProperties.getTemplateCode()).thenReturn("100001");
        lenient().when(smsProperties.getTemplateParam()).thenReturn("{\"code\":\"##code##\",\"min\":\"5\"}");
        lenient().when(smsProperties.getSchemeName()).thenReturn("test");
        lenient().when(smsProperties.getCodeLength()).thenReturn(6L);
        lenient().when(smsProperties.getRateLimitSeconds()).thenReturn(60L);
    }

    @Test
    void sendCode_whenRateLimited_shouldThrowBizException() {
        when(redisTemplate.hasKey("sms:rate:13800138000")).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> smsUtil.sendCode("13800138000"));
        assertEquals(ResultCode.SMS_CODE_FREQUENT.getCode(), ex.getCode());
        verifyNoInteractions(dypnsAsyncClient);
    }

    @Test
    void sendCode_whenSuccess_shouldCallAliyunAndSetRateLimit() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SendSmsVerifyCodeResponseBody body = mock(SendSmsVerifyCodeResponseBody.class);
        when(body.getSuccess()).thenReturn(true);
        when(body.getCode()).thenReturn("OK");
        SendSmsVerifyCodeResponse response = mock(SendSmsVerifyCodeResponse.class);
        when(response.getBody()).thenReturn(body);
        when(dypnsAsyncClient.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        smsUtil.sendCode("13800138000");

        ArgumentCaptor<SendSmsVerifyCodeRequest> captor = ArgumentCaptor.forClass(SendSmsVerifyCodeRequest.class);
        verify(dypnsAsyncClient).sendSmsVerifyCode(captor.capture());
        SendSmsVerifyCodeRequest req = captor.getValue();
        assertEquals("13800138000", req.getPhoneNumber());
        assertEquals("速通互联验证码", req.getSignName());
        assertEquals("100001", req.getTemplateCode());
        assertEquals("{\"code\":\"##code##\",\"min\":\"5\"}", req.getTemplateParam());
        assertEquals(Boolean.FALSE, req.getReturnVerifyCode());

        verify(valueOperations).set(eq("sms:rate:13800138000"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(valueOperations, never()).set(startsWith("sms:code:"), any(), anyLong(), any());
    }

    @Test
    void sendCode_whenAliyunFails_shouldThrowSendFailed() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        SendSmsVerifyCodeResponseBody body = mock(SendSmsVerifyCodeResponseBody.class);
        when(body.getSuccess()).thenReturn(false);
        when(body.getCode()).thenReturn("isv.BUSINESS_LIMIT_CONTROL");
        when(body.getMessage()).thenReturn("触发日发送限额");

        SendSmsVerifyCodeResponse response = mock(SendSmsVerifyCodeResponse.class);
        when(response.getBody()).thenReturn(body);
        when(dypnsAsyncClient.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        BizException ex = assertThrows(BizException.class, () -> smsUtil.sendCode("13800138000"));
        assertEquals(ResultCode.SMS_CODE_SEND_FAILED.getCode(), ex.getCode());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void verifyCode_whenPass_shouldReturnTrue() {
        CheckSmsVerifyCodeResponseBody.Model model = mock(CheckSmsVerifyCodeResponseBody.Model.class);
        when(model.getVerifyResult()).thenReturn("PASS");

        CheckSmsVerifyCodeResponseBody body = mock(CheckSmsVerifyCodeResponseBody.class);
        when(body.getSuccess()).thenReturn(true);
        when(body.getCode()).thenReturn("OK");
        when(body.getModel()).thenReturn(model);

        CheckSmsVerifyCodeResponse response = mock(CheckSmsVerifyCodeResponse.class);
        when(response.getBody()).thenReturn(body);
        when(dypnsAsyncClient.checkSmsVerifyCode(any(CheckSmsVerifyCodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        assertTrue(smsUtil.verifyCode("13800138000", "123456"));
    }

    @Test
    void verifyCode_whenUnknown_shouldReturnFalse() {
        CheckSmsVerifyCodeResponseBody.Model model = mock(CheckSmsVerifyCodeResponseBody.Model.class);
        when(model.getVerifyResult()).thenReturn("UNKNOWN");

        CheckSmsVerifyCodeResponseBody body = mock(CheckSmsVerifyCodeResponseBody.class);
        when(body.getSuccess()).thenReturn(true);
        when(body.getCode()).thenReturn("OK");
        when(body.getModel()).thenReturn(model);

        CheckSmsVerifyCodeResponse response = mock(CheckSmsVerifyCodeResponse.class);
        when(response.getBody()).thenReturn(body);
        when(dypnsAsyncClient.checkSmsVerifyCode(any(CheckSmsVerifyCodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        assertFalse(smsUtil.verifyCode("13800138000", "000000"));
    }
}
