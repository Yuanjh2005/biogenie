package com.agent.biogenie.client;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.AiProperties;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ChatRequestDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ChatStreamEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeAppClientTest {

    private DashScopeAppClient client;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
        client = new DashScopeAppClient(properties);
    }

    @Test
    void buildUrl_shouldAppendAppPath() {
        assertEquals(
                "https://dashscope.aliyuncs.com/api/v1/apps/app-123/completion",
                client.buildUrl("app-123")
        );
    }

    @Test
    void buildRequestBody_firstRound_shouldOnlyContainPrompt() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("请简单介绍一下 EGFR");

        String body = client.buildRequestBody(request, false);

        assertTrue(body.contains("\"prompt\":\"请简单介绍一下 EGFR\""));
        assertFalse(body.contains("session_id"));
        assertFalse(body.contains("parameters"));
    }

    @Test
    void buildRequestBody_stream_shouldEnableStreamAndIncrementalOutput() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("你好");

        String body = client.buildRequestBody(request, true);

        assertTrue(body.contains("\"stream\":true"));
        assertTrue(body.contains("\"incremental_output\":true"));
    }

    @Test
    void buildRequestBody_withSessionAndThinking_shouldIncludeFields() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("它和哪些疾病相关？");
        request.setSessionId("sess-001");
        request.setEnableThinking(true);
        request.setHasThoughts(true);

        String body = client.buildRequestBody(request, false);

        assertTrue(body.contains("\"session_id\":\"sess-001\""));
        assertTrue(body.contains("\"enable_thinking\":true"));
        assertTrue(body.contains("\"has_thoughts\":true"));
    }

    @Test
    void parseResponse_shouldMapOutputAndUsage() {
        String json = """
                {
                  "output": {
                    "text": "EGFR 是一种受体酪氨酸激酶",
                    "session_id": "sess-abc",
                    "finish_reason": "stop"
                  },
                  "usage": {
                    "models": [
                      {"input_tokens": 12, "output_tokens": 34}
                    ]
                  },
                  "request_id": "req-1"
                }
                """;
        ChatResultDTO result = client.parseResponse(json);

        assertEquals("EGFR 是一种受体酪氨酸激酶", result.getText());
        assertEquals("sess-abc", result.getSessionId());
        assertEquals("stop", result.getFinishReason());
        assertEquals("req-1", result.getRequestId());
        assertEquals(12, result.getInputTokens());
        assertEquals(34, result.getOutputTokens());
    }

    @Test
    void parseResponse_whenBusinessError_shouldThrow() {
        String json = """
                {"code":"InvalidApiKey","message":"Invalid API-key provided."}
                """;

        BizException ex = assertThrows(BizException.class, () -> client.parseResponse(json));
        assertEquals(ResultCode.AI_CALL_FAILED.getCode(), ex.getCode());
        assertEquals(ResultCode.AI_CALL_FAILED.getMessage(), ex.getMessage());
        assertFalse(ex.getMessage().contains("Invalid API-key"));
    }

    @Test
    void parseStreamData_delta_shouldMapIncrementalText() {
        String json = """
                {
                  "output": {
                    "text": "EGFR",
                    "session_id": "sess-1",
                    "finish_reason": "null"
                  },
                  "request_id": "req-s1"
                }
                """;

        ChatStreamEventDTO event = client.parseStreamData(json);

        assertEquals("delta", event.getType());
        assertEquals("EGFR", event.getText());
        assertEquals("sess-1", event.getSessionId());
        assertNull(event.getFinishReason());
    }

    @Test
    void parseStreamData_withThoughts_shouldMapThoughtItems() {
        String json = """
                {
                  "output": {
                    "session_id": "d2b6e0f2fdb748ea85c8e1e9692a57db",
                    "thoughts": [{
                      "action": "reasoning",
                      "thought": "看",
                      "action_type": "reasoning",
                      "response": "看",
                      "action_name": "思考过程"
                    }],
                    "finish_reason": "null",
                    "text": ""
                  },
                  "usage": {},
                  "request_id": "dd0fe306-2e50-9ce5-9164-a659dc116ed8"
                }
                """;

        ChatStreamEventDTO event = client.parseStreamData(json);

        assertEquals("delta", event.getType());
        assertEquals("", event.getText());
        assertNull(event.getFinishReason());
        assertNotNull(event.getThoughts());
        assertEquals(1, event.getThoughts().size());
        assertEquals("reasoning", event.getThoughts().get(0).getAction());
        assertEquals("看", event.getThoughts().get(0).getThought());
        assertEquals("reasoning", event.getThoughts().get(0).getActionType());
        assertEquals("看", event.getThoughts().get(0).getResponse());
        assertEquals("思考过程", event.getThoughts().get(0).getActionName());
    }

    @Test
    void parseStreamData_withDocReferences_shouldExposeSourceUrl() {
        String json = """
                {
                  "output": {
                    "session_id": "sess-web",
                    "finish_reason": "stop",
                    "text": "Answer[^1]",
                    "doc_references": [{
                      "index_id": "[1]",
                      "title": "EGF and skin repair",
                      "doc_id": "web-1",
                      "doc_name": "Example source",
                      "doc_url": "https://example.org/egf",
                      "text": "Source excerpt"
                    }]
                  },
                  "usage": {},
                  "request_id": "req-web"
                }
                """;

        ChatStreamEventDTO event = client.parseStreamData(json);

        assertEquals("done", event.getType());
        assertNotNull(event.getDocReferences());
        assertEquals(1, event.getDocReferences().size());
        assertEquals("[1]", event.getDocReferences().get(0).getIndexId());
        assertEquals("EGF and skin repair", event.getDocReferences().get(0).getTitle());
        assertEquals("https://example.org/egf", event.getDocReferences().get(0).getDocUrl());
    }

    @Test
    void parseResponse_withThoughts_shouldMapThoughtItems() {
        String json = """
                {
                  "output": {
                    "text": "最终答案",
                    "session_id": "sess-t",
                    "finish_reason": "stop",
                    "thoughts": [{
                      "action": "reasoning",
                      "thought": "先分析",
                      "action_type": "reasoning",
                      "response": "先分析",
                      "action_name": "思考过程"
                    }]
                  },
                  "request_id": "req-t"
                }
                """;

        ChatResultDTO result = client.parseResponse(json);

        assertEquals("最终答案", result.getText());
        assertEquals("stop", result.getFinishReason());
        assertNotNull(result.getThoughts());
        assertEquals("先分析", result.getThoughts().get(0).getThought());
    }

    @Test
    void parseStreamData_stop_shouldBeDone() {
        String json = """
                {
                  "output": {
                    "text": "完成。",
                    "session_id": "sess-1",
                    "finish_reason": "stop"
                  },
                  "usage": {
                    "models": [{"input_tokens": 5, "output_tokens": 8}]
                  },
                  "request_id": "req-s2"
                }
                """;

        ChatStreamEventDTO event = client.parseStreamData(json);

        assertEquals("done", event.getType());
        assertEquals("sess-1", event.getSessionId());
        assertEquals(5, event.getInputTokens());
        assertEquals(8, event.getOutputTokens());
    }

    @Test
    void parseStreamData_businessError_shouldBeErrorEvent() {
        String json = """
                {"code":"InvalidParameter","message":"prompt is required"}
                """;

        ChatStreamEventDTO event = client.parseStreamData(json);

        assertEquals("error", event.getType());
        assertEquals(ResultCode.AI_CALL_FAILED.getMessage(), event.getMessage());
        assertFalse(event.getMessage().contains("prompt is required"));
    }

    @Test
    void buildRequestBody_withImageAttachment_shouldIncludeImageList() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("图片的内容是什么");
        request.setAttachments(List.of(ChatAttachmentDTO.builder()
                .url("https://gitee.com/fengmou11123/picgo/raw/master/images/20260723152133795.png")
                .name("20260723152133795.png")
                .contentType("image/png")
                .build()));

        String body = client.buildRequestBody(request, false);

        assertTrue(body.contains("\"image_list\":[\"https://gitee.com/fengmou11123/picgo/raw/master/images/20260723152133795.png\"]"));
        assertFalse(body.contains("file_list"));
    }

    @Test
    void buildRequestBody_withPdfAttachment_shouldIncludeFileList() {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setPrompt("总结文件");
        request.setAttachments(List.of(ChatAttachmentDTO.builder()
                .url("https://example.com/a.pdf")
                .name("a.pdf")
                .contentType("application/pdf")
                .build()));

        String body = client.buildRequestBody(request, false);

        assertTrue(body.contains("\"file_list\":[\"https://example.com/a.pdf\"]"));
        assertFalse(body.contains("image_list"));
    }

    @Test
    void truncateForLog_shouldCapLength() {
        String longBody = "x".repeat(600);
        String truncated = client.truncateForLog(longBody);
        assertTrue(truncated.length() < longBody.length());
        assertTrue(truncated.contains("truncated"));
        assertTrue(truncated.startsWith("x".repeat(512)));
    }
}
