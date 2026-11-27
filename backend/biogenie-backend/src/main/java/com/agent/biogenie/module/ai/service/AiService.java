package com.agent.biogenie.module.ai.service;

import com.agent.biogenie.dto.ai.ChatRequestDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务
 */
public interface AiService {

    /**
     * 同步调用百炼应用完成一轮对话（支持多轮 sessionId）
     */
    ChatResultDTO chat(ChatRequestDTO request);

    /**
     * SSE 流式对话：异步写入 emitter，方法立即返回
     */
    void chatStream(ChatRequestDTO request, SseEmitter emitter);
}
