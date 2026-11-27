package com.agent.biogenie.module.admin.service;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.user.SecondFactorTokenDTO;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.utils.SmsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminSecondFactorService {
    private static final String PREFIX = "admin:confirm:";
    private static final long TTL_SECONDS = 300;

    private final UserMapper userMapper;
    private final SmsUtil smsUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public String sendCode(Long userId) {
        User user = requirePhone(userId);
        smsUtil.sendCode(user.getPhone());
        return maskPhone(user.getPhone());
    }

    public SecondFactorTokenDTO verify(Long userId, String sessionId, String code) {
        User user = requirePhone(userId);
        if (!smsUtil.verifyCode(user.getPhone(), code)) {
            throw new BizException(ResultCode.SMS_CODE_ERROR);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(PREFIX + token, userId + ":" + sessionId,
                TTL_SECONDS, TimeUnit.SECONDS);
        return new SecondFactorTokenDTO(token, TTL_SECONDS);
    }

    /** 令牌单次使用，校验时立即删除。 */
    public void consume(Long userId, String sessionId, String token) {
        if (StrUtil.isBlank(token)) {
            throw new BizException(ResultCode.SECOND_FACTOR_REQUIRED);
        }
        Object value = redisTemplate.opsForValue().getAndDelete(PREFIX + token);
        if (!String.valueOf(userId + ":" + sessionId).equals(String.valueOf(value))) {
            throw new BizException(ResultCode.SECOND_FACTOR_INVALID);
        }
    }

    private User requirePhone(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        if (StrUtil.isBlank(user.getPhone())) {
            throw new BizException(ResultCode.BAD_REQUEST, "当前管理员未绑定手机号，无法进行二次确认");
        }
        return user;
    }

    private String maskPhone(String phone) {
        return phone.length() >= 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : "****";
    }
}
