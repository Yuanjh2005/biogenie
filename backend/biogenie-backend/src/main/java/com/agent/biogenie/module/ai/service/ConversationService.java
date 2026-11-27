package com.agent.biogenie.module.ai.service;

import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.ai.ConversationSessionDTO;
import com.agent.biogenie.dto.admin.AdminConversationSessionDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * AI 会话持久化与查询
 */
public interface ConversationService {

    /**
     * 续聊前校验 session 归属：库中已有则必须属于当前用户且未归档；未落库的历史 session 放行。
     */
    void assertSessionAccessible(Long userId, String sessionId);

    /**
     * 百炼调用成功后落库：创建/更新会话并写入本轮问答。
     *
     * @param attachments 本轮附件元数据，可为 null
     */
    void saveTurn(Long userId, String appKey, String question, ChatResultDTO result,
                  List<ChatAttachmentDTO> attachments);

    /**
     * 分页查询当前用户 ACTIVE 会话。
     */
    Page<ConversationSessionDTO> pageSessions(Long userId, int current, int size, String keyword);

    /** 管理员分页查看所有用户会话（含已归档）。 */
    Page<AdminConversationSessionDTO> pageAllSessions(int current, int size, String keyword);

    /**
     * 查询会话历史消息（按时间升序）。
     */
    List<ConversationMessageDTO> listMessages(Long userId, String sessionId);

    /** 管理员只读查看任意未删除会话的消息。 */
    List<ConversationMessageDTO> listAllMessages(String sessionId);

    /**
     * 归档会话。
     */
    void archive(Long userId, String sessionId);

    /**
     * 逻辑删除会话及其消息。
     */
    void delete(Long userId, String sessionId);

}
