package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "admin.log")
public class AdminLogProperties {
    private int behaviorRetentionDays = 90;
    private int auditRetentionDays = 365;
    private int cleanupBatchSize = 5000;
    private int cleanupMaxBatches = 100;
    private int behaviorQueueCapacity = 20000;
    private int behaviorFlushSize = 500;
}
