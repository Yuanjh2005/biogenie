package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.user.SecondFactorTokenDTO;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.utils.SmsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSecondFactorServiceTest {
    @Mock UserMapper userMapper;
    @Mock SmsUtil smsUtil;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> values;
    private AdminSecondFactorService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(values);
        service = new AdminSecondFactorService(userMapper, smsUtil, redisTemplate);
    }

    @Test
    void verifyShouldIssueSessionBoundToken() {
        User user = new User();
        user.setId(1L); user.setPhone("13800138000");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(smsUtil.verifyCode("13800138000", "123456")).thenReturn(true);

        SecondFactorTokenDTO result = service.verify(1L, "sid-1", "123456");

        assertNotNull(result.getConfirmToken());
        assertEquals(300, result.getExpiresInSeconds());
        verify(values).set(startsWith("admin:confirm:"), eq("1:sid-1"), eq(300L), any());
    }

    @Test
    void consumeShouldRejectTokenFromAnotherSession() {
        when(values.getAndDelete("admin:confirm:token")).thenReturn("1:sid-old");
        BizException ex = assertThrows(BizException.class, () -> service.consume(1L, "sid-new", "token"));
        assertEquals(ResultCode.SECOND_FACTOR_INVALID.getCode(), ex.getCode());
    }
}
