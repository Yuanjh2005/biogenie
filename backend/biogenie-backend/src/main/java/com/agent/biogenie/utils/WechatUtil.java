package com.agent.biogenie.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.WechatProperties;
import com.agent.biogenie.dto.user.WechatQrConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 微信开放平台 PC 端扫码登录工具类
 * <p>
 * 适用于「网站应用」类型的 AppID，使用 qrconnect + snsapi_login 流程。
 * 文档: https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatUtil {

    /** PC 端扫码登录授权地址 */
    private static final String QR_CONNECT_URL = "https://open.weixin.qq.com/connect/qrconnect";
    /** 通过 code 换取 access_token */
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    /** 获取用户信息 */
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    /** PC 端扫码登录固定 scope */
    private static final String SCOPE_LOGIN = "snsapi_login";

    private static final String STATE_PREFIX = "wechat:state:";
    private static final long STATE_EXPIRE_MINUTES = 5;

    private final WechatProperties wechatProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成 PC 端扫码登录配置（供前端展示二维码）
     */
    public WechatQrConfigDTO buildQrConfig() {
        validateConfig();

        String state = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(STATE_PREFIX + state, "1", STATE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        String redirectUri = wechatProperties.getRedirectUri();
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String qrConnectUrl = QR_CONNECT_URL
                + "?appid=" + wechatProperties.getAppId()
                + "&redirect_uri=" + encodedRedirectUri
                + "&response_type=code"
                + "&scope=" + SCOPE_LOGIN
                + "&state=" + state
                + "#wechat_redirect";

        return WechatQrConfigDTO.builder()
                .appId(wechatProperties.getAppId())
                .redirectUri(redirectUri)
                .scope(SCOPE_LOGIN)
                .state(state)
                .qrConnectUrl(qrConnectUrl)
                .build();
    }

    /**
     * 校验 state 防 CSRF
     */
    public void validateState(String state) {
        if (StrUtil.isBlank(state)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "state 不能为空");
        }
        String key = STATE_PREFIX + state;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new BizException(ResultCode.WECHAT_CODE_INVALID, "state 无效或已过期");
        }
        redisTemplate.delete(key);
    }

    /**
     * 通过授权 code 换取 access_token 和 openid
     */
    public JSONObject getAccessToken(String code) {
        validateConfig();

        String url = ACCESS_TOKEN_URL
                + "?appid=" + wechatProperties.getAppId()
                + "&secret=" + wechatProperties.getAppSecret()
                + "&code=" + code
                + "&grant_type=authorization_code";

        String response;
        try {
            response = HttpUtil.get(url);
        } catch (Exception e) {
            log.error("微信开放平台 API 请求失败", e);
            throw new BizException(ResultCode.WECHAT_LOGIN_FAILED, "微信服务请求失败");
        }

        JSONObject respJson = JSONUtil.parseObj(response);
        if (respJson.containsKey("errcode") && respJson.getInt("errcode") != 0) {
            log.error("微信 PC 端登录换取 token 失败: errcode={}, errmsg={}",
                    respJson.getInt("errcode"), respJson.getStr("errmsg", "未知错误"));
            throw new BizException(ResultCode.WECHAT_CODE_INVALID,
                    "微信授权失败: " + respJson.getStr("errmsg", "未知错误"));
        }
        return respJson;
    }

    /**
     * 获取微信用户基本信息
     */
    public JSONObject getUserInfo(String accessToken, String openid) {
        String url = USER_INFO_URL
                + "?access_token=" + accessToken
                + "&openid=" + openid
                + "&lang=zh_CN";

        try {
            String response = HttpUtil.get(url);
            JSONObject userInfo = JSONUtil.parseObj(response);
            if (userInfo.containsKey("errcode") && userInfo.getInt("errcode") != 0) {
                log.warn("获取微信用户信息失败: errcode={}, errmsg={}",
                        userInfo.getInt("errcode"), userInfo.getStr("errmsg", "未知错误"));
                LogUtils.logBusinessError("WECHAT_USERINFO_FAIL", "微信API返回错误", null);
                return new JSONObject();
            }
            return userInfo;
        } catch (Exception e) {
            log.warn("获取微信用户信息失败: {}", e.getMessage());
            LogUtils.logBusinessError("WECHAT_USERINFO_FAIL", "微信用户信息请求异常", e);
            return new JSONObject();
        }
    }

    private void validateConfig() {
        if (StrUtil.hasBlank(wechatProperties.getAppId(), wechatProperties.getAppSecret(),
                wechatProperties.getRedirectUri())) {
            throw new BizException(ResultCode.WECHAT_LOGIN_FAILED, "微信开放平台配置不完整，请检查 app-id、app-secret、redirect-uri");
        }
    }
}
