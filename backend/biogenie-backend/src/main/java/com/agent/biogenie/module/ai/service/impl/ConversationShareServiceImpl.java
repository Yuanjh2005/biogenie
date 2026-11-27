package com.agent.biogenie.module.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.ShareProperties;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.ai.ConversationShareCreateDTO;
import com.agent.biogenie.dto.ai.ConversationShareViewDTO;
import com.agent.biogenie.dto.ai.ConversationShareViewDTO.ShareAttachmentDTO;
import com.agent.biogenie.dto.ai.ConversationShareViewDTO.ShareMessageDTO;
import com.agent.biogenie.module.ai.entity.ConversationShare;
import com.agent.biogenie.module.ai.mapper.ConversationShareMapper;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.agent.biogenie.module.ai.service.ConversationShareService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话分享：整段快照 + 固定 12h 有效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationShareServiceImpl implements ConversationShareService {

    private static final int EXPIRE_HOURS = 12;

    private final ConversationShareMapper shareMapper;
    private final ConversationService conversationService;
    private final ShareProperties shareProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationShareCreateDTO create(Long userId, String sessionId) {
        List<ConversationMessageDTO> messages = conversationService.listMessages(userId, sessionId);
        if (messages == null || messages.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "会话暂无消息，无法分享");
        }

        String title = resolveTitle(messages);
        List<ShareMessageDTO> snapshot = toSnapshot(messages);
        LocalDateTime expireAt = LocalDateTime.now().plusHours(EXPIRE_HOURS);
        String shareToken = IdUtil.nanoId(22);

        ConversationShare share = new ConversationShare();
        share.setShareToken(shareToken);
        share.setUserId(userId);
        share.setSessionId(sessionId);
        share.setTitle(title);
        share.setSnapshotJson(JSONUtil.toJsonStr(snapshot));
        share.setStatus(ConversationShare.STATUS_ACTIVE);
        share.setExpireAt(expireAt);
        shareMapper.insert(share);

        return ConversationShareCreateDTO.builder()
                .shareToken(shareToken)
                .shareUrl(buildShareUrl(shareToken))
                .expireAt(expireAt)
                .title(title)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationShareViewDTO getByToken(String shareToken) {
        if (StrUtil.isBlank(shareToken)) {
            throw new BizException(ResultCode.SHARE_NOT_FOUND);
        }
        ConversationShare share = shareMapper.selectOne(
                new LambdaQueryWrapper<ConversationShare>()
                        .eq(ConversationShare::getShareToken, shareToken.trim())
                        .last("LIMIT 1"));
        if (share == null) {
            throw new BizException(ResultCode.SHARE_NOT_FOUND);
        }

        boolean expired = share.getStatus() == null
                || share.getStatus() == ConversationShare.STATUS_EXPIRED
                || (share.getExpireAt() != null && !LocalDateTime.now().isBefore(share.getExpireAt()));
        if (expired) {
            markExpired(share);
            throw new BizException(ResultCode.SHARE_EXPIRED);
        }

        List<ShareMessageDTO> messages = parseSnapshot(share.getSnapshotJson());
        return ConversationShareViewDTO.builder()
                .title(share.getTitle())
                .expireAt(share.getExpireAt())
                .messages(messages)
                .build();
    }

    private void markExpired(ConversationShare share) {
        if (share.getStatus() != null && share.getStatus() == ConversationShare.STATUS_EXPIRED) {
            return;
        }
        ConversationShare patch = new ConversationShare();
        patch.setId(share.getId());
        patch.setStatus(ConversationShare.STATUS_EXPIRED);
        patch.setUpdateTime(LocalDateTime.now());
        shareMapper.updateById(patch);
    }

    private String buildShareUrl(String shareToken) {
        String base = StrUtil.blankToDefault(shareProperties.getBaseUrl(), "http://localhost:3000").trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/share/" + shareToken;
    }

    private String resolveTitle(List<ConversationMessageDTO> messages) {
        ConversationMessageDTO first = messages.get(0);
        if (first != null && StrUtil.isNotBlank(first.getQuestion())) {
            String q = first.getQuestion().trim();
            return q.length() <= 40 ? q : q.substring(0, 40);
        }
        return "对话分享";
    }

    private List<ShareMessageDTO> toSnapshot(List<ConversationMessageDTO> messages) {
        List<ShareMessageDTO> list = new ArrayList<>(messages.size());
        for (ConversationMessageDTO msg : messages) {
            if (msg == null) {
                continue;
            }
            list.add(ShareMessageDTO.builder()
                    .question(msg.getQuestion())
                    .answer(msg.getAnswer())
                    .createTime(msg.getCreateTime())
                    .attachments(toShareAttachments(msg.getAttachments()))
                    .build());
        }
        return list;
    }

    private List<ShareAttachmentDTO> toShareAttachments(List<ChatAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        List<ShareAttachmentDTO> list = new ArrayList<>();
        for (ChatAttachmentDTO att : attachments) {
            if (att == null) {
                continue;
            }
            if (StrUtil.isBlank(att.getName()) && StrUtil.isBlank(att.getContentType())) {
                continue;
            }
            list.add(ShareAttachmentDTO.builder()
                    .name(att.getName())
                    .contentType(att.getContentType())
                    .build());
        }
        return list.isEmpty() ? null : list;
    }

    private List<ShareMessageDTO> parseSnapshot(String snapshotJson) {
        if (StrUtil.isBlank(snapshotJson)) {
            return List.of();
        }
        try {
            List<ShareMessageDTO> list = JSONUtil.toList(snapshotJson, ShareMessageDTO.class);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("解析分享快照失败: {}", e.getMessage());
            throw new BizException(ResultCode.INTERNAL_ERROR, "分享内容解析失败");
        }
    }
}
