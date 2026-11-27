package com.agent.biogenie.module.ai.mapper;

import com.agent.biogenie.dto.admin.AdminConversationSessionDTO;
import com.agent.biogenie.module.ai.entity.ConversationSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI 会话 Mapper
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {

    @Select("""
            <script>
            SELECT c.session_id, c.user_id, u.nickname AS user_nickname,
                   u.phone AS user_phone, c.title, c.app_key, c.status,
                   c.create_time, c.update_time
            FROM t_conversation_session c
            LEFT JOIN t_user u ON u.id = c.user_id AND u.deleted = 0
            WHERE c.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (
                c.title LIKE CONCAT('%', #{keyword}, '%')
                OR c.session_id LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                OR CAST(c.user_id AS CHAR) = #{keyword}
              )
            </if>
            ORDER BY c.update_time DESC, c.id DESC
            </script>
            """)
    Page<AdminConversationSessionDTO> selectAdminPage(
            Page<AdminConversationSessionDTO> page,
            @Param("keyword") String keyword);
}
