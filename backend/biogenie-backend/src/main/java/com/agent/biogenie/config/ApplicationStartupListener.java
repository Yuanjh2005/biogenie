package com.agent.biogenie.config;

import com.agent.biogenie.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 应用启动日志
 */
@Slf4j
@Component
public class ApplicationStartupListener {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String[] profiles = env.getActiveProfiles();
        String profile = profiles.length > 0 ? String.join(",", profiles) : "default";
        int port = env.getProperty("server.port", Integer.class, 8080);

        LogUtils.logSystemStart("BiogenieBackendApplication", "SUCCESS",
                "port=" + port + ", profile=" + profile);
        log.info("BioGenie启动完成, port={}, profile={}", port, profile);
    }
}
