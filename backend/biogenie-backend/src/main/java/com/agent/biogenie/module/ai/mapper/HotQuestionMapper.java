package com.agent.biogenie.module.ai.mapper;

import com.agent.biogenie.module.ai.entity.HotQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 热门问题 Mapper
 */
@Mapper
public interface HotQuestionMapper extends BaseMapper<HotQuestion> {

    /**
     * 命中 +1；已存在则累加并刷新展示文案
     */
    @Insert("""
            INSERT INTO t_hot_question (app_key, question_key, display_question, hit_count)
            VALUES (#{appKey}, #{questionKey}, #{displayQuestion}, 1)
            ON DUPLICATE KEY UPDATE
              hit_count = hit_count + 1,
              display_question = VALUES(display_question),
              update_time = CURRENT_TIMESTAMP
            """)
    int upsertHit(@Param("appKey") String appKey,
                   @Param("questionKey") String questionKey,
                   @Param("displayQuestion") String displayQuestion);
}
