package com.agent.biogenie.module.ai.service.impl;

import com.agent.biogenie.dto.ai.HotQuestionDTO;
import com.agent.biogenie.module.ai.entity.HotQuestion;
import com.agent.biogenie.module.ai.mapper.HotQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotQuestionServiceImplTest {

    @Mock
    private HotQuestionMapper hotQuestionMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private HotQuestionServiceImpl hotQuestionService;

    @Test
    void record_shouldUpsertMysqlAndIncrRedis() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        hotQuestionService.record("smartdrgf-fast", "  " + "什么是" + "  " + "EGFR" + "？  ");

        verify(hotQuestionMapper).upsertHit("smartdrgf-fast", "什么是 egfr", "什么是  EGFR？");
        verify(zSetOperations).incrementScore("ai:hot:questions:smartdrgf-fast", "什么是 egfr", 1);
        verify(hashOperations).put("ai:hot:questions:display:smartdrgf-fast", "什么是 egfr", "什么是  EGFR？");
    }

    @Test
    void record_tooShort_shouldSkip() {
        hotQuestionService.record("smartdrgf-fast", "a");
        verifyNoInteractions(hotQuestionMapper);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void listTop_whenRedisInsufficient_shouldLoadMysqlAndBackfill() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.zCard("ai:hot:questions:global")).thenReturn(0L);

        HotQuestion row = new HotQuestion();
        row.setAppKey("");
        row.setQuestionKey("什么是 egfr");
        row.setDisplayQuestion("什么是EGFR");
        row.setHitCount(9L);
        when(hotQuestionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(row));

        List<HotQuestionDTO> list = hotQuestionService.listTop(null, 3);

        assertEquals(1, list.size());
        assertEquals("什么是EGFR", list.get(0).getQuestion());
        assertEquals(9L, list.get(0).getHitCount());
        verify(zSetOperations).add("ai:hot:questions:global", "什么是 egfr", 9.0);
        verify(hashOperations).put("ai:hot:questions:display:global", "什么是 egfr", "什么是EGFR");
    }

    @Test
    void listTop_whenQuestionTooLong_shouldReturnFullStoredQuestion() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.zCard("ai:hot:questions:global")).thenReturn(0L);

        HotQuestion row = new HotQuestion();
        row.setAppKey("");
        row.setQuestionKey("long-question");
        String fullQuestion = "这是一个超过三十个字的热门问题展示文案用于验证接口返回时不会进行截断处理并由前端控制展示长度";
        row.setDisplayQuestion(fullQuestion);
        row.setHitCount(12L);
        when(hotQuestionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(row));

        List<HotQuestionDTO> list = hotQuestionService.listTop(null, 3);

        assertEquals(1, list.size());
        assertEquals(fullQuestion, list.get(0).getQuestion());
        assertEquals(12L, list.get(0).getHitCount());
    }
}
