package com.agent.biogenie.module.ai.service.impl;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.ShareProperties;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.ai.ConversationShareCreateDTO;
import com.agent.biogenie.dto.ai.ConversationShareViewDTO;
import com.agent.biogenie.module.ai.entity.ConversationShare;
import com.agent.biogenie.module.ai.mapper.ConversationShareMapper;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationShareServiceImplTest {

    @Mock
    private ConversationShareMapper shareMapper;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ShareProperties shareProperties;

    @InjectMocks
    private ConversationShareServiceImpl shareService;

    @BeforeEach
    void setUp() {
        lenient().when(shareProperties.getBaseUrl()).thenReturn("https://app.example.com/");
    }

    @Test
    void create_emptyMessages_shouldFail() {
        when(conversationService.listMessages(1L, "s1")).thenReturn(List.of());

        BizException ex = assertThrows(BizException.class, () -> shareService.create(1L, "s1"));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(shareMapper, never()).insert(any(ConversationShare.class));
    }

    @Test
    void create_shouldSnapshotWithoutAttachmentUrl_andExpireIn12Hours() {
        ConversationMessageDTO msg = ConversationMessageDTO.builder()
                .id(10L)
                .question("什么是EGFR")
                .answer("EGFR是...")
                .createTime(LocalDateTime.of(2026, 1, 1, 10, 0))
                .attachments(List.of(ChatAttachmentDTO.builder()
                        .url("https://oss.example/a.pdf")
                        .name("a.pdf")
                        .contentType("application/pdf")
                        .build()))
                .requestId("req-1")
                .build();
        when(conversationService.listMessages(1L, "s1")).thenReturn(List.of(msg));

        LocalDateTime before = LocalDateTime.now();
        ConversationShareCreateDTO result = shareService.create(1L, "s1");
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result.getShareToken());
        assertEquals(22, result.getShareToken().length());
        assertEquals("https://app.example.com/share/" + result.getShareToken(), result.getShareUrl());
        assertEquals("什么是EGFR", result.getTitle());
        assertFalse(result.getExpireAt().isBefore(before.plusHours(12).minusSeconds(2)));
        assertFalse(result.getExpireAt().isAfter(after.plusHours(12).plusSeconds(2)));

        ArgumentCaptor<ConversationShare> captor = ArgumentCaptor.forClass(ConversationShare.class);
        verify(shareMapper).insert(captor.capture());
        ConversationShare saved = captor.getValue();
        assertEquals(ConversationShare.STATUS_ACTIVE, saved.getStatus());
        assertTrue(saved.getSnapshotJson().contains("什么是EGFR"));
        assertTrue(saved.getSnapshotJson().contains("a.pdf"));
        assertFalse(saved.getSnapshotJson().contains("https://oss.example"));
        assertFalse(saved.getSnapshotJson().contains("req-1"));
    }

    @Test
    void getByToken_active_shouldReturnSnapshot() {
        ConversationShare share = new ConversationShare();
        share.setId(1L);
        share.setShareToken("tok123");
        share.setTitle("标题");
        share.setStatus(ConversationShare.STATUS_ACTIVE);
        share.setExpireAt(LocalDateTime.now().plusHours(6));
        share.setSnapshotJson("[{\"question\":\"Q\",\"answer\":\"A\"}]");
        when(shareMapper.selectOne(any(Wrapper.class))).thenReturn(share);

        ConversationShareViewDTO view = shareService.getByToken("tok123");

        assertEquals("标题", view.getTitle());
        assertEquals(1, view.getMessages().size());
        assertEquals("Q", view.getMessages().get(0).getQuestion());
        verify(shareMapper, never()).update(any(), any());
    }

    @Test
    void getByToken_expired_shouldMarkStatusZero() {
        ConversationShare share = new ConversationShare();
        share.setId(2L);
        share.setShareToken("oldtok");
        share.setStatus(ConversationShare.STATUS_ACTIVE);
        share.setExpireAt(LocalDateTime.now().minusMinutes(1));
        share.setSnapshotJson("[]");
        when(shareMapper.selectOne(any(Wrapper.class))).thenReturn(share);

        BizException ex = assertThrows(BizException.class, () -> shareService.getByToken("oldtok"));
        assertEquals(ResultCode.SHARE_EXPIRED.getCode(), ex.getCode());

        ArgumentCaptor<ConversationShare> captor = ArgumentCaptor.forClass(ConversationShare.class);
        verify(shareMapper).updateById(captor.capture());
        assertEquals(ConversationShare.STATUS_EXPIRED, captor.getValue().getStatus());
        assertEquals(2L, captor.getValue().getId());
    }

    @Test
    void getByToken_alreadyExpiredStatus_shouldNotUpdateAgain() {
        ConversationShare share = new ConversationShare();
        share.setId(3L);
        share.setShareToken("dead");
        share.setStatus(ConversationShare.STATUS_EXPIRED);
        share.setExpireAt(LocalDateTime.now().minusHours(1));
        when(shareMapper.selectOne(any(Wrapper.class))).thenReturn(share);

        BizException ex = assertThrows(BizException.class, () -> shareService.getByToken("dead"));
        assertEquals(ResultCode.SHARE_EXPIRED.getCode(), ex.getCode());
        verify(shareMapper, never()).updateById(any(ConversationShare.class));
    }

    @Test
    void getByToken_missing_shouldNotFound() {
        when(shareMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> shareService.getByToken("nope"));
        assertEquals(ResultCode.SHARE_NOT_FOUND.getCode(), ex.getCode());
    }
}
