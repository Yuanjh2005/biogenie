package com.agent.biogenie.module.admin.mapper;

import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ApiBehaviorLogMapper extends BaseMapper<ApiBehaviorLog> {
    @Insert("""
            <script>
            INSERT INTO t_api_behavior_log
              (user_id, user_role, request_method, request_path, status_code,
               duration_ms, ip, user_agent, create_time)
            VALUES
            <foreach collection='rows' item='row' separator=','>
              (#{row.userId}, #{row.userRole}, #{row.requestMethod}, #{row.requestPath},
               #{row.statusCode}, #{row.durationMs}, #{row.ip}, #{row.userAgent}, #{row.createTime})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("rows") List<ApiBehaviorLog> rows);

    @Delete("""
            DELETE FROM t_api_behavior_log
            WHERE create_time &lt; #{expireTime}
            ORDER BY create_time
            LIMIT #{batchSize}
            """)
    int deleteExpired(@Param("expireTime") LocalDateTime expireTime,
                      @Param("batchSize") int batchSize);
}
