package com.agent.biogenie.utils;

import cn.hutool.core.util.StrUtil;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码工具：对接阿里云号码认证 SendSmsVerifyCode / CheckSmsVerifyCode
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsUtil {

    private static final String SMS_RATE_PREFIX = "sms:rate:";
    private static final String VERIFY_PASS = "PASS";

    private final SmsProperties smsProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AsyncClient dypnsAsyncClient;

    /**
     * 发送短信验证码（验证码由阿里云生成并保管）
     */
    public void sendCode(String phone) {
        validateSendConfig();

        String rateKey = SMS_RATE_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            log.warn("短信发送过于频繁");
            throw new BizException(ResultCode.SMS_CODE_FREQUENT);
        }

        SendSmsVerifyCodeRequest.Builder requestBuilder = SendSmsVerifyCodeRequest.builder()
                .phoneNumber(phone)
                .signName(smsProperties.getSignName())
                .templateCode(smsProperties.getTemplateCode())
                .templateParam(smsProperties.getTemplateParam())
                .codeLength(smsProperties.getCodeLength())
                .returnVerifyCode(false);

        if (StrUtil.isNotBlank(smsProperties.getSchemeName())) {
            requestBuilder.schemeName(smsProperties.getSchemeName());
        }

        try {
            SendSmsVerifyCodeResponse response = dypnsAsyncClient
                    .sendSmsVerifyCode(requestBuilder.build())
                    .get();
            SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess()) || !"OK".equalsIgnoreCase(body.getCode())) {
                String message = body == null ? "无响应" : StrUtil.blankToDefault(body.getMessage(), body.getCode());
                log.error("发送短信验证码失败: {}", message);
                throw new BizException(ResultCode.SMS_CODE_SEND_FAILED, "短信发送失败: " + message);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用阿里云发送短信失败: {}", e.getMessage(), e);
            throw new BizException(ResultCode.SMS_CODE_SEND_FAILED, "短信发送失败");
        }

        redisTemplate.opsForValue().set(rateKey, "1", smsProperties.getRateLimitSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 核验短信验证码（阿里云 CheckSmsVerifyCode）
     *
     * @return true 表示核验通过
     */
    public boolean verifyCode(String phone, String code) {
        if (StrUtil.hasBlank(phone, code)) {
            return false;
        }
        validateSendConfig();

        CheckSmsVerifyCodeRequest.Builder requestBuilder = CheckSmsVerifyCodeRequest.builder()
                .phoneNumber(phone)
                .verifyCode(code);

        if (StrUtil.isNotBlank(smsProperties.getSchemeName())) {
            requestBuilder.schemeName(smsProperties.getSchemeName());
        }

        try {
            CheckSmsVerifyCodeResponse response = dypnsAsyncClient
                    .checkSmsVerifyCode(requestBuilder.build())
                    .get();
            CheckSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess()) || !"OK".equalsIgnoreCase(body.getCode())) {
                String message = body == null ? "无响应" : StrUtil.blankToDefault(body.getMessage(), body.getCode());
                log.warn("短信验证码核验接口失败: {}", message);
                return false;
            }
            CheckSmsVerifyCodeResponseBody.Model model = body.getModel();
            boolean pass = model != null && VERIFY_PASS.equalsIgnoreCase(model.getVerifyResult());
            if (!pass) {
                log.warn("短信验证码核验未通过");
            }
            return pass;
        } catch (Exception e) {
            log.error("调用阿里云核验短信失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private void validateSendConfig() {
        if (StrUtil.hasBlank(
                smsProperties.getAccessKeyId(),
                smsProperties.getAccessKeySecret(),
                smsProperties.getSignName(),
                smsProperties.getTemplateCode(),
                smsProperties.getTemplateParam())) {
            throw new BizException(ResultCode.SMS_CODE_SEND_FAILED, "短信服务配置不完整");
        }
        if (smsProperties.getAccessKeyId().startsWith("your-")
                || smsProperties.getAccessKeySecret().startsWith("your-")) {
            throw new BizException(ResultCode.SMS_CODE_SEND_FAILED, "请先配置真实的短信 AccessKey");
        }
    }
}
