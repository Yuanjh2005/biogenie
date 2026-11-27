package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.dto.admin.OperationsOverviewDTO;
import com.agent.biogenie.module.ai.entity.ConversationMessage;
import com.agent.biogenie.module.ai.entity.ConversationSession;
import com.agent.biogenie.module.ai.mapper.ConversationMessageMapper;
import com.agent.biogenie.module.ai.mapper.ConversationSessionMapper;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationsStatisticsService {
    private static final int STATISTICS_DAYS = 30;
    private final UserMapper userMapper;
    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;

    public OperationsOverviewDTO overview() {
        LocalDateTime from = LocalDateTime.now().minusDays(STATISTICS_DAYS);
        Map<String, Object> tokenSums = messageMapper.selectMaps(
                new QueryWrapper<ConversationMessage>()
                        .select("COALESCE(SUM(input_tokens),0) AS input_tokens",
                                "COALESCE(SUM(output_tokens),0) AS output_tokens")
                        .ge("create_time", from)).stream().findFirst().orElse(Map.of());

        return OperationsOverviewDTO.builder()
                .days(STATISTICS_DAYS)
                .from(from)
                .totalUsers(userMapper.selectCount(null))
                .enabledUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1)))
                .newUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreateTime, from)))
                .activeUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getLastLoginTime, from)))
                .admins(userMapper.selectCount(new LambdaQueryWrapper<User>().in(User::getRole, "ADMIN", "SUPER_ADMIN")))
                .totalConversations(sessionMapper.selectCount(null))
                .newConversations(sessionMapper.selectCount(new LambdaQueryWrapper<ConversationSession>()
                        .ge(ConversationSession::getCreateTime, from)))
                .messages(messageMapper.selectCount(new LambdaQueryWrapper<ConversationMessage>()
                        .ge(ConversationMessage::getCreateTime, from)))
                .inputTokens(number(tokenSums, "input_tokens"))
                .outputTokens(number(tokenSums, "output_tokens"))
                .build();
    }

    private long number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) value = map.get(key.toUpperCase());
        return value instanceof Number n ? n.longValue() : new BigDecimal(String.valueOf(value == null ? 0 : value)).longValue();
    }
}
