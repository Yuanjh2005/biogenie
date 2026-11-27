package com.agent.biogenie.module.ai.service.impl;

import com.agent.biogenie.client.DashScopeAppClient;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.common.auth.AuthConstants;
import com.agent.biogenie.config.AiProperties;
import com.agent.biogenie.dto.ai.ChatRequestDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.agent.biogenie.utils.MinioUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private DashScopeAppClient dashScopeAppClient;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private java.util.concurrent.ExecutorService aiStreamExecutor;

    private AiProperties aiProperties;
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setDefaultApp("smartdrgf-fast");

        AiProperties.AppCredential fast = new AiProperties.AppCredential();
        fast.setAppId("app-fast-id");
        fast.setApiKey("sk-real-key");

        Map<String, AiProperties.AppCredential> apps = new LinkedHashMap<>();
        apps.put("smartdrgf-fast", fast);
        aiProperties.setApps(apps);

        aiService = new AiServiceImpl(aiProperties, dashScopeAppClient, conversationService, minioUtil, aiStreamExecutor);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthConstants.ATTR_USER_ID, 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void chat_shouldUseDefaultAppForwardSessionAndPersist() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("它和哪些疾病相关？");
        request.setSessionId("sess-1");

        ChatResultDTO expected = ChatResultDTO.builder()
                .text("相关疾病包括...")
                .sessionId("sess-1")
                .build();
        when(dashScopeAppClient.completion(eq("app-fast-id"), eq("sk-real-key"), eq(request)))
                .thenReturn(expected);

        ChatResultDTO result = aiService.chat(request);

        assertEquals("相关疾病包括...", result.getText());
        assertEquals("sess-1", result.getSessionId());
        verify(conversationService).assertSessionAccessible(1L, "sess-1");
        verify(dashScopeAppClient).completion("app-fast-id", "sk-real-key", request);
        verify(conversationService).saveTurn(1L, "smartdrgf-fast", "它和哪些疾病相关？", expected, null);
    }

    @Test
    void chat_whenUnknownApp_shouldThrowConfigError() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("hello");
        request.setApp("unknown-app");

        BizException ex = assertThrows(BizException.class, () -> aiService.chat(request));
        assertEquals(ResultCode.AI_CONFIG_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(dashScopeAppClient);
        verify(conversationService, never()).saveTurn(any(), any(), any(), any(), any());
    }

    @Test
    void chat_whenPlaceholderCredential_shouldThrowConfigError() {
        AiProperties.AppCredential placeholder = new AiProperties.AppCredential();
        placeholder.setAppId("your-app-id");
        placeholder.setApiKey("your-api-key");
        aiProperties.getApps().put("smartdrgf-fast", placeholder);

        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("hello");

        BizException ex = assertThrows(BizException.class, () -> aiService.chat(request));
        assertEquals(ResultCode.AI_CONFIG_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(dashScopeAppClient);
    }
}
