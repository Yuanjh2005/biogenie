package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS 跨域配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /** 是否允许所有来源（开发环境） */
    private boolean allowAll = false;

    /** 生产环境允许的具体域名 */
    private List<String> allowedOrigins = new ArrayList<>();

    /** 是否允许携带凭证 */
    private boolean allowCredentials = true;
}
