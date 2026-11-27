package com.agent.biogenie.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminSchemaInitializerTest {
    @Test
    void shouldEnsureBehaviorLogTable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        new AdminSchemaInitializer(jdbcTemplate).run(null);
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS `t_api_behavior_log`"));
    }
}
