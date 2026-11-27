package com.agent.biogenie.module.admin.mapper;

import com.agent.biogenie.module.admin.entity.AdminAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLog> {
    @Delete("""
            DELETE FROM t_admin_audit_log
            WHERE create_time &lt; #{expireTime}
            ORDER BY create_time
            LIMIT #{batchSize}
            """)
    int deleteExpired(@Param("expireTime") LocalDateTime expireTime,
                      @Param("batchSize") int batchSize);
}
