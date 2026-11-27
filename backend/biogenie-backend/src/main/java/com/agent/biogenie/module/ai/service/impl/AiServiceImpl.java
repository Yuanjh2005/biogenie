package com.agent.biogenie.module.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.client.DashScopeAppClient;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.common.auth.AuthContext;
import com.agent.biogenie.config.AiProperties;
import com.agent.biogenie.config.AiStreamExecutorConfig;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ChatRequestDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ChatStreamEventDTO;
import com.agent.biogenie.module.ai.service.AiService;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.agent.biogenie.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 对话服务实现：代理阿里云百炼应用 completion，并落库问答
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final AiProperties aiProperties;
    private final DashScopeAppClient dashScopeAppClient;
    private final ConversationService conversationService;
    private final MinioUtil minioUtil;
    private final ExecutorService aiStreamExecutor;

    public AiServiceImpl(AiProperties aiProperties,
                         DashScopeAppClient dashScopeAppClient,
                         ConversationService conversationService,
                         MinioUtil minioUtil,
                         @Qualifier(AiStreamExecutorConfig.BEAN_NAME) ExecutorService aiStreamExecutor) {
        this.aiProperties = aiProperties;
        this.dashScopeAppClient = dashScopeAppClient;
        this.conversationService = conversationService;
        this.minioUtil = minioUtil;
        this.aiStreamExecutor = aiStreamExecutor;
    }

    @Override
    public ChatResultDTO chat(ChatRequestDTO request) {
        Long userId = AuthContext.getUserId();
        String appKey = StrUtil.blankToDefault(request.getApp(), aiProperties.getDefaultApp());
        AiProperties.AppCredential credential = resolveCredential(appKey);

        conversationService.assertSessionAccessible(userId, request.getSessionId());
        resignAttachmentsForUpstream(request);

        ChatResultDTO result = dashScopeAppClient.completion(
                credential.getAppId(), credential.getApiKey(), request);

        try {
            conversationService.saveTurn(userId, appKey, request.getPrompt(), result, request.getAttachments());
        } catch (Exception e) {
            log.error("AI 对话落库失败: {}", e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void chatStream(ChatRequestDTO request, SseEmitter emitter) {
        Long userId = AuthContext.getUserId();
        String appKey = StrUtil.blankToDefault(request.getApp(), aiProperties.getDefaultApp());
        AiProperties.AppCredential credential = resolveCredential(appKey);

        conversationService.assertSessionAccessible(userId, request.getSessionId());
        resignAttachmentsForUpstream(request);

        try {
            aiStreamExecutor.execute(() -> runStreamTask(request, emitter, userId, appKey, credential));
        } catch (RejectedExecutionException e) {
            log.warn("AI 流式线程池已满，拒绝请求: app={}", appKey);
            sendEvent(emitter, ChatStreamEventDTO.builder()
                    .type("error")
                    .message("当前流式对话繁忙，请稍后重试")
                    .build());
            emitter.complete();
        }
    }

    private void runStreamTask(ChatRequestDTO request, SseEmitter emitter, Long userId,
                               String appKey, AiProperties.AppCredential credential) {
        StringBuilder answerBuffer = new StringBuilder();
        AtomicReference<ChatStreamEventDTO> doneRef = new AtomicReference<>();
        try {
            dashScopeAppClient.completionStream(
                    credential.getAppId(),
                    credential.getApiKey(),
                    request,
                    event -> {
                        if ("delta".equals(event.getType()) && StrUtil.isNotBlank(event.getText())) {
                            String prev = answerBuffer.toString();
                            String chunk = event.getText();
                            if (prev.isEmpty()) {
                                answerBuffer.append(chunk);
                            } else if (chunk.startsWith(prev)) {
                                answerBuffer.setLength(0);
                                answerBuffer.append(chunk);
                            } else if (!prev.startsWith(chunk)) {
                                answerBuffer.append(chunk);
                            }
                        } else if ("done".equals(event.getType())) {
                            if (StrUtil.isNotBlank(event.getText()) && answerBuffer.isEmpty()) {
                                answerBuffer.append(event.getText());
                            }
                            doneRef.set(event);
                        }
                        sendEvent(emitter, event);
                    }
            );

            ChatStreamEventDTO done = doneRef.get();
            if (done != null && StrUtil.isNotBlank(done.getSessionId())) {
                ChatResultDTO result = ChatResultDTO.builder()
                        .text(answerBuffer.toString())
                        .docReferences(done.getDocReferences())
                        .sessionId(done.getSessionId())
                        .finishReason(done.getFinishReason())
                        .requestId(done.getRequestId())
                        .inputTokens(done.getInputTokens())
                        .outputTokens(done.getOutputTokens())
                        .build();
                try {
                    conversationService.saveTurn(userId, appKey, request.getPrompt(), result, request.getAttachments());
                } catch (Exception e) {
                    log.error("AI 流式对话落库失败: {}", e.getMessage(), e);
                }
            }

            emitter.complete();
        } catch (BizException e) {
            log.warn("AI 流式对话业务失败: {}", e.getMessage());
            sendEvent(emitter, ChatStreamEventDTO.builder()
                    .type("error")
                    .message(e.getMessage())
                    .build());
            emitter.complete();
        } catch (Exception e) {
            log.error("AI 流式对话异常", e);
            sendEvent(emitter, ChatStreamEventDTO.builder()
                    .type("error")
                    .message("AI 流式调用失败")
                    .build());
            emitter.completeWithError(e);
        }
    }

    private void sendEvent(SseEmitter emitter, ChatStreamEventDTO event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(event));
        } catch (IOException e) {
            log.debug("SSE 客户端已断开: {}", e.getMessage());
            throw new IllegalStateException("SSE 发送失败", e);
        }
    }

    private AiProperties.AppCredential resolveCredential(String appKey) {
        if (aiProperties.getApps() == null || aiProperties.getApps().isEmpty()) {
            throw new BizException(ResultCode.AI_CONFIG_ERROR, "未配置 AI 应用凭证");
        }
        AiProperties.AppCredential credential = aiProperties.getApps().get(appKey);
        if (credential == null) {
            throw new BizException(ResultCode.AI_CONFIG_ERROR, "未知的 AI 应用: " + appKey);
        }
        if (StrUtil.hasBlank(credential.getAppId(), credential.getApiKey())) {
            throw new BizException(ResultCode.AI_CONFIG_ERROR, "AI 应用凭证不完整: " + appKey);
        }
        if (credential.getAppId().startsWith("your-") || credential.getApiKey().startsWith("your-")) {
            throw new BizException(ResultCode.AI_CONFIG_ERROR, "请先填写 AI 应用凭证: " + appKey);
        }
        return credential;
    }

    private int attachmentCount(ChatRequestDTO request) {
        return request.getAttachments() == null ? 0 : request.getAttachments().size();
    }

    /**
     * 发给百炼前：对本桶裸链/过期签名换签；外链保持原样（去 query 后仍是完整 URL）。
     */
    private void resignAttachmentsForUpstream(ChatRequestDTO request) {
        List<ChatAttachmentDTO> attachments = request.getAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (ChatAttachmentDTO att : attachments) {
            if (att != null && StrUtil.isNotBlank(att.getUrl())) {
                att.setUrl(minioUtil.toAccessUrl(att.getUrl()));
            }
        }
    }
}
