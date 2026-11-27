package com.agent.biogenie.module.ai.service;

import com.agent.biogenie.dto.ai.ConversationShareCreateDTO;
import com.agent.biogenie.dto.ai.ConversationShareViewDTO;

/**
 * AI 对话分享
 */
public interface ConversationShareService {

    /**
     * 将整段会话固化为快照并生成 12h 有效分享链接
     */
    ConversationShareCreateDTO create(Long userId, String sessionId);

    /**
     * 按 token 打开分享；过期时将 status 置 0 并抛出 SHARE_EXPIRED
     */
    ConversationShareViewDTO getByToken(String shareToken);
}
