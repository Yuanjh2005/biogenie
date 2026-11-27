package com.agent.biogenie.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationsOverviewDTO {
    private int days;
    private LocalDateTime from;
    private long totalUsers;
    private long enabledUsers;
    private long newUsers;
    private long activeUsers;
    private long admins;
    private long totalConversations;
    private long newConversations;
    private long messages;
    private long inputTokens;
    private long outputTokens;
}
