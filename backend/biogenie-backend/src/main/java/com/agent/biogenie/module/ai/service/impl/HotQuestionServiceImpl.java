package com.agent.biogenie.module.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.dto.ai.HotQuestionDTO;
import com.agent.biogenie.module.ai.entity.HotQuestion;
import com.agent.biogenie.module.ai.mapper.HotQuestionMapper;
import com.agent.biogenie.module.ai.service.HotQuestionService;
import com.agent.biogenie.module.ai.support.HotQuestionNormalizer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 热门问题：写时双写 MySQL + Redis；读时 Redis 优先，不足则 MySQL 补全回填
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotQuestionServiceImpl implements HotQuestionService {

    private static final String ZSET_PREFIX = "ai:hot:questions:";
    private static final String DISPLAY_HASH_PREFIX = "ai:hot:questions:display:";
    /** Redis miss 时从 MySQL 回填的条数 */
    private static final int BACKFILL_SIZE = 50;

    private final HotQuestionMapper hotQuestionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void record(String appKey, String question) {
        String questionKey = HotQuestionNormalizer.normalize(question);
        if (questionKey == null) {
            return;
        }
        String normalizedApp = HotQuestionNormalizer.normalizeAppKey(appKey);
        String display = HotQuestionNormalizer.toDisplay(question);
        if (StrUtil.isBlank(display)) {
            return;
        }

        try {
            hotQuestionMapper.upsertHit(normalizedApp, questionKey, display);
        } catch (Exception e) {
            log.warn("热门问题 MySQL 累加失败: app={}, key={}", normalizedApp, questionKey, e);
        }

        try {
            stringRedisTemplate.opsForZSet().incrementScore(zsetKey(normalizedApp), questionKey, 1);
            stringRedisTemplate.opsForHash().put(displayHashKey(normalizedApp), questionKey, display);
        } catch (Exception e) {
            log.warn("热门问题 Redis 累加失败: app={}, key={}", normalizedApp, questionKey, e);
        }
    }

    @Override
    public List<HotQuestionDTO> listTop(String appKey, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        String normalizedApp = HotQuestionNormalizer.normalizeAppKey(appKey);

        List<HotQuestionDTO> fromRedis = readFromRedis(normalizedApp, safeLimit);
        if (fromRedis.size() >= safeLimit) {
            return fromRedis;
        }

        List<HotQuestion> fromDb = loadFromMysql(normalizedApp, BACKFILL_SIZE);
        if (!fromDb.isEmpty()) {
            backfillRedis(normalizedApp, fromDb);
        }

        List<HotQuestionDTO> result = new ArrayList<>();
        for (HotQuestion row : fromDb) {
            if (result.size() >= safeLimit) {
                break;
            }
            result.add(HotQuestionDTO.builder()
                    .question(row.getDisplayQuestion())
                    .hitCount(row.getHitCount())
                    .build());
        }
        return result;
    }

    private List<HotQuestionDTO> readFromRedis(String appKey, int limit) {
        try {
            String zsetKey = zsetKey(appKey);
            Long card = stringRedisTemplate.opsForZSet().zCard(zsetKey);
            if (card == null || card < limit) {
                return Collections.emptyList();
            }

            Set<ZSetOperations.TypedTuple<String>> tuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, limit - 1);
            if (tuples == null || tuples.size() < limit) {
                return Collections.emptyList();
            }

            List<String> keys = new ArrayList<>();
            List<Long> scores = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null) {
                    continue;
                }
                keys.add(tuple.getValue());
                scores.add(tuple.getScore() == null ? 0L : tuple.getScore().longValue());
            }
            if (keys.size() < limit) {
                return Collections.emptyList();
            }

            Map<String, String> displays = resolveDisplays(appKey, keys);
            List<HotQuestionDTO> list = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                String qKey = keys.get(i);
                String display = displays.getOrDefault(qKey, qKey);
                list.add(HotQuestionDTO.builder()
                        .question(display)
                        .hitCount(scores.get(i))
                        .build());
            }
            return list;
        } catch (Exception e) {
            log.warn("热门问题 Redis 读取失败: app={}", appKey, e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> resolveDisplays(String appKey, List<String> keys) {
        Map<String, String> result = new HashMap<>();
        List<Object> hashValues = stringRedisTemplate.opsForHash()
                .multiGet(displayHashKey(appKey), new ArrayList<>(keys));
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            Object v = hashValues == null || i >= hashValues.size() ? null : hashValues.get(i);
            if (v == null || StrUtil.isBlank(String.valueOf(v))) {
                missing.add(keys.get(i));
            } else {
                result.put(keys.get(i), String.valueOf(v));
            }
        }
        if (missing.isEmpty()) {
            return result;
        }

        List<HotQuestion> rows = hotQuestionMapper.selectList(
                new LambdaQueryWrapper<HotQuestion>()
                        .eq(HotQuestion::getAppKey, appKey)
                        .in(HotQuestion::getQuestionKey, missing));
        for (HotQuestion row : rows) {
            result.put(row.getQuestionKey(), row.getDisplayQuestion());
            try {
                stringRedisTemplate.opsForHash()
                        .put(displayHashKey(appKey), row.getQuestionKey(), row.getDisplayQuestion());
            } catch (Exception ignored) {
                // ignore
            }
        }
        return result;
    }

    private List<HotQuestion> loadFromMysql(String appKey, int size) {
        try {
            return hotQuestionMapper.selectList(
                    new LambdaQueryWrapper<HotQuestion>()
                            .eq(HotQuestion::getAppKey, appKey)
                            .orderByDesc(HotQuestion::getHitCount)
                            .orderByDesc(HotQuestion::getUpdateTime)
                            .last("LIMIT " + size));
        } catch (Exception e) {
            log.warn("热门问题 MySQL 查询失败: app={}", appKey, e);
            return Collections.emptyList();
        }
    }

    private void backfillRedis(String appKey, List<HotQuestion> rows) {
        try {
            String zsetKey = zsetKey(appKey);
            String hashKey = displayHashKey(appKey);
            for (HotQuestion row : rows) {
                if (StrUtil.isBlank(row.getQuestionKey()) || row.getHitCount() == null) {
                    continue;
                }
                stringRedisTemplate.opsForZSet().add(zsetKey, row.getQuestionKey(), row.getHitCount());
                if (StrUtil.isNotBlank(row.getDisplayQuestion())) {
                    stringRedisTemplate.opsForHash().put(hashKey, row.getQuestionKey(), row.getDisplayQuestion());
                }
            }
        } catch (Exception e) {
            log.warn("热门问题 Redis 回填失败: app={}", appKey, e);
        }
    }

    private static String zsetKey(String appKey) {
        return ZSET_PREFIX + (StrUtil.isBlank(appKey) ? "global" : appKey);
    }

    private static String displayHashKey(String appKey) {
        return DISPLAY_HASH_PREFIX + (StrUtil.isBlank(appKey) ? "global" : appKey);
    }
}
