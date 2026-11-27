package com.agent.biogenie.module.ai.service;

import com.agent.biogenie.dto.ai.HotQuestionDTO;

import java.util.List;

/**
 * 热门问题统计与查询
 */
public interface HotQuestionService {

    /**
     * 对话成功后累加热度（失败仅记日志，不抛异常）
     */
    void record(String appKey, String question);

    /**
     * 查询热门 TopN：先 Redis，不足则 MySQL 补全并回填
     */
    List<HotQuestionDTO> listTop(String appKey, int limit);
}
