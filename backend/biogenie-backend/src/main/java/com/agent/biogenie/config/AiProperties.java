package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云百炼（DashScope）应用调用配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 百炼 API 根路径 */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";

    private int connectTimeoutMs = 10000;

    private int readTimeoutMs = 60000;

    /** 上游流式读超时（毫秒），流式生成可能较长 */
    private int streamReadTimeoutMs = 300000;

    /** 对前端 SseEmitter 超时（毫秒） */
    private int streamTimeoutMs = 300000;

    /** 流式任务线程池（有界，防止无界扩容） */
    private StreamPool streamPool = new StreamPool();

    /** 默认应用配置名，对应 apps 中的 key */
    private String defaultApp = "smartdrgf-fast";

    /** 多应用凭证：key 为业务侧 app 名 */
    private Map<String, AppCredential> apps = new LinkedHashMap<>();

    @Data
    public static class AppCredential {
        private String appId;
        private String apiKey;
    }

    @Data
    public static class StreamPool {
        /** 核心线程数 */
        private int coreSize = 16;
        /** 最大线程数 */
        private int maxSize = 32;
        /** 空闲线程存活秒数 */
        private int keepAliveSeconds = 60;
        /** 有界队列容量 */
        private int queueCapacity = 128;
    }
}
