package com.agent.biogenie.module.ai.service.impl;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.admin.AdminConversationSessionDTO;
import com.agent.biogenie.module.ai.entity.ConversationMessage;
import com.agent.biogenie.module.ai.entity.ConversationSession;
import com.agent.biogenie.module.ai.mapper.ConversationMessageMapper;
import com.agent.biogenie.module.ai.mapper.ConversationSessionMapper;
import com.agent.biogenie.module.ai.service.HotQuestionService;
import com.agent.biogenie.utils.MinioUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationSessionMapper sessionMapper;

    @Mock
    private ConversationMessageMapper messageMapper;

    @Mock
    private HotQuestionService hotQuestionService;

    @Mock
    private MinioUtil minioUtil;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    @Test
    void assertSessionAccessible_whenOwned_shouldPass() {
        ConversationSession session = new ConversationSession();
        session.setSessionId("s1");
        session.setUserId(1L);
        session.setStatus(ConversationSession.STATUS_ACTIVE);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        assertDoesNotThrow(() -> conversationService.assertSessionAccessible(1L, "s1"));
    }

    @Test
    void assertSessionAccessible_whenOtherUser_shouldForbid() {
        ConversationSession session = new ConversationSession();
        session.setSessionId("s1");
        session.setUserId(2L);
        session.setStatus(ConversationSession.STATUS_ACTIVE);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        BizException ex = assertThrows(BizException.class,
                () -> conversationService.assertSessionAccessible(1L, "s1"));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void saveTurn_whenNewSession_shouldInsertSessionAndMessage() {
        when(sessionMapper.selectOne(any())).thenReturn(null);

        ChatResultDTO result = ChatResultDTO.builder()
                .text("答案")
                .sessionId("sess-new")
                .requestId("req-1")
                .finishReason("stop")
                .inputTokens(10)
                .outputTokens(20)
                .build();

        conversationService.saveTurn(1L, "smartdrgf-fast", "什么是EGFR？", result, null);

        ArgumentCaptor<ConversationSession> sessionCaptor = ArgumentCaptor.forClass(ConversationSession.class);
        verify(sessionMapper).insert(sessionCaptor.capture());
        assertEquals("sess-new", sessionCaptor.getValue().getSessionId());
        assertEquals(1L, sessionCaptor.getValue().getUserId());
        assertEquals("什么是EGFR？", sessionCaptor.getValue().getTitle());

        ArgumentCaptor<ConversationMessage> messageCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(messageMapper).insert(messageCaptor.capture());
        assertEquals("什么是EGFR？", messageCaptor.getValue().getQuestion());
        assertEquals("答案", messageCaptor.getValue().getAnswer());
        assertEquals("sess-new", messageCaptor.getValue().getSessionId());
        assertNull(messageCaptor.getValue().getAttachmentsJson());
        verify(hotQuestionService).record("smartdrgf-fast", "什么是EGFR？");
    }

    @Test
    void saveTurn_withAttachments_shouldPersistAttachmentsJson() {
        when(sessionMapper.selectOne(any())).thenReturn(null);
        when(minioUtil.toPermanentUrl(anyString())).thenAnswer(inv -> {
            String url = inv.getArgument(0);
            int q = url.indexOf('?');
            return q >= 0 ? url.substring(0, q) : url;
        });

        ChatResultDTO result = ChatResultDTO.builder()
                .text("答案")
                .sessionId("sess-att")
                .build();
        var attachments = List.of(com.agent.biogenie.dto.ai.ChatAttachmentDTO.builder()
                .url("http://127.0.0.1:9000/biogenie/a.pdf?X-Amz-Signature=old")
                .name("a.pdf")
                .contentType("application/pdf")
                .build());

        conversationService.saveTurn(1L, "design-mcp", "总结一下", result, attachments);

        ArgumentCaptor<ConversationMessage> messageCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(messageMapper).insert(messageCaptor.capture());
        String json = messageCaptor.getValue().getAttachmentsJson();
        assertNotNull(json);
        assertTrue(json.contains("http://127.0.0.1:9000/biogenie/a.pdf"));
        assertFalse(json.contains("X-Amz-Signature=old"));
        assertTrue(json.contains("a.pdf"));
    }

    @Test
    void listMessages_whenOwned_shouldReturnOrderedAndResignAttachments() {
        ConversationSession session = new ConversationSession();
        session.setId(9L);
        session.setSessionId("s1");
        session.setUserId(1L);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        ConversationMessage m1 = new ConversationMessage();
        m1.setId(1L);
        m1.setSessionId("s1");
        m1.setQuestion("q1");
        m1.setAnswer("a1");
        m1.setAttachmentsJson(
                "[{\"url\":\"http://127.0.0.1:9000/biogenie/images/a.png\",\"name\":\"a.png\",\"contentType\":\"image/png\"}]");
        when(messageMapper.selectList(any())).thenReturn(List.of(m1));
        when(minioUtil.toAccessUrl("http://127.0.0.1:9000/biogenie/images/a.png"))
                .thenReturn("http://127.0.0.1:9000/biogenie/images/a.png?X-Amz-Signature=new");

        List<ConversationMessageDTO> list = conversationService.listMessages(1L, "s1");
        assertEquals(1, list.size());
        assertEquals("q1", list.get(0).getQuestion());
        assertNotNull(list.get(0).getAttachments());
        assertEquals("http://127.0.0.1:9000/biogenie/images/a.png?X-Amz-Signature=new",
                list.get(0).getAttachments().get(0).getUrl());
    }

    @Test
    void pageAllSessions_shouldTrimKeywordAndMaskPhone() {
        AdminConversationSessionDTO item = new AdminConversationSessionDTO();
        item.setSessionId("s1");
        item.setUserPhone("13800138000");
        Page<AdminConversationSessionDTO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(item));
        when(sessionMapper.selectAdminPage(any(), eq("EGFR"))).thenReturn(page);

        Page<AdminConversationSessionDTO> result =
                conversationService.pageAllSessions(1, 10, "  EGFR  ");

        assertEquals("138****8000", result.getRecords().get(0).getUserPhone());
    }

    @Test
    void pageSessions_shouldQueryOnlyTheCurrentUsersActiveSessions() {
        Page<ConversationSession> page = new Page<>(1, 10, 0);
        when(sessionMapper.selectPage(any(), any())).thenReturn(page);

        var result = conversationService.pageSessions(1L, 1, 10, " EGFR ");

        assertEquals(0, result.getTotal());
        verify(sessionMapper).selectPage(any(), any());
    }

    @Test
    void listAllMessages_shouldAllowAdminReadWithoutOwnerArgument() {
        ConversationSession session = new ConversationSession();
        session.setSessionId("s-other");
        session.setUserId(22L);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        ConversationMessage message = new ConversationMessage();
        message.setId(7L);
        message.setSessionId("s-other");
        message.setQuestion("q");
        message.setAnswer("a");
        when(messageMapper.selectList(any())).thenReturn(List.of(message));

        List<ConversationMessageDTO> result = conversationService.listAllMessages("s-other");

        assertEquals(1, result.size());
        assertEquals("q", result.get(0).getQuestion());
    }

    @Test
    void delete_shouldRemoveMessagesAndSession() {
        ConversationSession session = new ConversationSession();
        session.setId(9L);
        session.setSessionId("s1");
        session.setUserId(1L);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        conversationService.delete(1L, "s1");

        verify(messageMapper).delete(any(Wrapper.class));
        verify(sessionMapper).deleteById(9L);
    }

}
