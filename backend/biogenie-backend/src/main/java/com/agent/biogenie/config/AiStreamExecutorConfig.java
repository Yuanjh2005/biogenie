package com.agent.biogenie.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AI 流式对话有界线程池，避免 CachedThreadPool 无界扩容打爆 JVM。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiStreamExecutorConfig {

    public static final String BEAN_NAME = "aiStreamExecutor";

    private final AiProperties aiProperties;

    private ThreadPoolExecutor executor;

    @Bean(name = BEAN_NAME)
    public ExecutorService aiStreamExecutor() {
        AiProperties.StreamPool pool = aiProperties.getStreamPool();
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("ai-stream-");
        threadFactory.setDaemon(true);

        executor = new ThreadPoolExecutor(
                pool.getCoreSize(),
                pool.getMaxSize(),
                pool.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(pool.getQueueCapacity()),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(false);
        log.info("AI 流式线程池已启动: core={}, max={}, queue={}, keepAliveSeconds={}",
                pool.getCoreSize(), pool.getMaxSize(), pool.getQueueCapacity(), pool.getKeepAliveSeconds());
        return executor;
    }

    @PreDestroy
    public void shutdown() {
        if (executor == null) {
            return;
        }
        log.info("正在关闭 AI 流式线程池...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
