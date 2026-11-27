package com.agent.biogenie.module.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.ai.ConversationSessionDTO;
import com.agent.biogenie.dto.admin.AdminConversationSessionDTO;
import com.agent.biogenie.module.ai.entity.ConversationMessage;
import com.agent.biogenie.module.ai.entity.ConversationSession;
import com.agent.biogenie.module.ai.mapper.ConversationMessageMapper;
import com.agent.biogenie.module.ai.mapper.ConversationSessionMapper;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.agent.biogenie.module.ai.service.HotQuestionService;
import com.agent.biogenie.utils.LogMaskUtil;
import com.agent.biogenie.utils.MinioUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 会话持久化实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final int TITLE_MAX_LEN = 40;

    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final HotQuestionService hotQuestionService;
    private final MinioUtil minioUtil;

    @Override
    public void assertSessionAccessible(Long userId, String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        ConversationSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ConversationSession>()
                        .eq(ConversationSession::getSessionId, sessionId)
                        .last("LIMIT 1"));
        if (session == null) {
            // 历史未落库的百炼 session，允许继续调用，成功后再写入
            return;
        }
        if (!userId.equals(session.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        if (session.getStatus() != null && session.getStatus() == ConversationSession.STATUS_ARCHIVED) {
            throw new BizException(ResultCode.CONVERSATION_ARCHIVED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTurn(Long userId, String appKey, String question, ChatResultDTO result,
                         List<ChatAttachmentDTO> attachments) {
        if (userId == null || result == null || StrUtil.isBlank(result.getSessionId())) {
            log.warn("跳过会话落库: sessionId 为空");
            return;
        }
        if (StrUtil.isBlank(question) || StrUtil.isBlank(result.getText())) {
            log.warn("跳过会话落库: question/answer 为空");
            return;
        }

        String sessionId = result.getSessionId();
        ConversationSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<ConversationSession>()
                        .eq(ConversationSession::getSessionId, sessionId)
                        .last("LIMIT 1"));

        if (existing == null) {
            ConversationSession session = new ConversationSession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setTitle(buildTitle(question));
            session.setAppKey(appKey);
            session.setStatus(ConversationSession.STATUS_ACTIVE);
            try {
                sessionMapper.insert(session);
            } catch (DuplicateKeyException e) {
                existing = getOwnedSession(userId, sessionId);
                touchSession(existing.getId());
            }
        } else {
            if (!userId.equals(existing.getUserId())) {
                throw new BizException(ResultCode.FORBIDDEN, "无权写入该会话");
            }
            touchSession(existing.getId());
        }

        ConversationMessage message = new ConversationMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setQuestion(question);
        message.setAnswer(result.getText());
        message.setAttachmentsJson(toAttachmentsJson(attachments));
        message.setRequestId(result.getRequestId());
        message.setFinishReason(result.getFinishReason());
        message.setInputTokens(result.getInputTokens());
        message.setOutputTokens(result.getOutputTokens());
        messageMapper.insert(message);

        try {
            hotQuestionService.record(appKey, question);
        } catch (Exception e) {
            log.warn("热门问题记录失败: app={}", appKey, e);
        }
    }

    @Override
    public Page<ConversationSessionDTO> pageSessions(Long userId, int current, int size, String keyword) {
        Page<ConversationSession> page = sessionMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<ConversationSession>()
                        .eq(ConversationSession::getUserId, userId)
                        .eq(ConversationSession::getStatus, ConversationSession.STATUS_ACTIVE)
                        .like(StrUtil.isNotBlank(keyword), ConversationSession::getTitle, StrUtil.trim(keyword))
                        .orderByDesc(ConversationSession::getUpdateTime));

        Page<ConversationSessionDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toSessionDto).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    public Page<AdminConversationSessionDTO> pageAllSessions(int current, int size, String keyword) {
        Page<AdminConversationSessionDTO> page = sessionMapper.selectAdminPage(
                new Page<>(current, size), StrUtil.trimToNull(keyword));
        page.getRecords().forEach(record ->
                record.setUserPhone(LogMaskUtil.maskPhone(record.getUserPhone())));
        return page;
    }

    @Override
    public List<ConversationMessageDTO> listMessages(Long userId, String sessionId) {
        getOwnedSession(userId, sessionId);
        List<ConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getSessionId, sessionId)
                        .eq(ConversationMessage::getUserId, userId)
                        .orderByAsc(ConversationMessage::getCreateTime)
                        .orderByAsc(ConversationMessage::getId));
        return messages.stream().map(this::toMessageDto).collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessageDTO> listAllMessages(String sessionId) {
        ConversationSession session = getSession(sessionId);
        List<ConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getSessionId, sessionId)
                        .eq(ConversationMessage::getUserId, session.getUserId())
                        .orderByAsc(ConversationMessage::getCreateTime)
                        .orderByAsc(ConversationMessage::getId));
        return messages.stream().map(this::toMessageDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long userId, String sessionId) {
        ConversationSession session = getOwnedSession(userId, sessionId);
        sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSession>()
                .eq(ConversationSession::getId, session.getId())
                .set(ConversationSession::getStatus, ConversationSession.STATUS_ARCHIVED)
                .set(ConversationSession::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String sessionId) {
        ConversationSession session = getOwnedSession(userId, sessionId);
        messageMapper.delete(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getSessionId, sessionId)
                .eq(ConversationMessage::getUserId, userId));
        sessionMapper.deleteById(session.getId());
    }

    private ConversationSession getOwnedSession(Long userId, String sessionId) {
        ConversationSession session = getSession(sessionId);
        if (!userId.equals(session.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }

    private ConversationSession getSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "sessionId 不能为空");
        }
        ConversationSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ConversationSession>()
                        .eq(ConversationSession::getSessionId, sessionId)
                        .last("LIMIT 1"));
        if (session == null) {
            throw new BizException(ResultCode.CONVERSATION_NOT_FOUND);
        }
        return session;
    }

    private void touchSession(Long id) {
        sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSession>()
                .eq(ConversationSession::getId, id)
                .set(ConversationSession::getUpdateTime, LocalDateTime.now()));
    }

    private String buildTitle(String question) {
        String normalized = StrUtil.trim(question);
        if (StrUtil.isBlank(normalized)) {
            return "新对话";
        }
        if (normalized.length() <= TITLE_MAX_LEN) {
            return normalized;
        }
        return normalized.substring(0, TITLE_MAX_LEN);
    }

    private ConversationSessionDTO toSessionDto(ConversationSession session) {
        return ConversationSessionDTO.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .appKey(session.getAppKey())
                .status(session.getStatus())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private ConversationMessageDTO toMessageDto(ConversationMessage message) {
        return ConversationMessageDTO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .question(message.getQuestion())
                .answer(message.getAnswer())
                .attachments(parseAttachmentsJson(message.getAttachmentsJson()))
                .requestId(message.getRequestId())
                .finishReason(message.getFinishReason())
                .inputTokens(message.getInputTokens())
                .outputTokens(message.getOutputTokens())
                .createTime(message.getCreateTime())
                .build();
    }

    private String toAttachmentsJson(List<ChatAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        List<Map<String, String>> list = new ArrayList<>();
        for (ChatAttachmentDTO att : attachments) {
            if (att == null || StrUtil.isBlank(att.getUrl())) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            // 落库存裸链，避免预签名过期写入 DB
            item.put("url", minioUtil.toPermanentUrl(att.getUrl()));
            if (StrUtil.isNotBlank(att.getName())) {
                item.put("name", att.getName());
            }
            if (StrUtil.isNotBlank(att.getContentType())) {
                item.put("contentType", att.getContentType());
            }
            list.add(item);
        }
        return list.isEmpty() ? null : JSONUtil.toJsonStr(list);
    }

    private List<ChatAttachmentDTO> parseAttachmentsJson(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            List<ChatAttachmentDTO> list = JSONUtil.toList(json, ChatAttachmentDTO.class);
            if (list == null || list.isEmpty()) {
                return null;
            }
            for (ChatAttachmentDTO att : list) {
                if (att != null && StrUtil.isNotBlank(att.getUrl())) {
                    att.setUrl(minioUtil.toAccessUrl(att.getUrl()));
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("解析 attachments_json 失败: {}", e.getMessage());
            return null;
        }
    }
}
