package com.agent.biogenie.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.config.AiProperties;
import com.agent.biogenie.dto.ai.ChatAttachmentDTO;
import com.agent.biogenie.dto.ai.ChatRequestDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ChatStreamEventDTO;
import com.agent.biogenie.dto.ai.DocReferenceDTO;
import com.agent.biogenie.dto.ai.ThoughtItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 阿里云百炼应用调用客户端
 * <p>
 * POST https://dashscope.aliyuncs.com/api/v1/apps/{app_id}/completion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeAppClient {

    /** 对外固定文案，不透传上游 message */
    private static final String CLIENT_ERROR_MESSAGE = ResultCode.AI_CALL_FAILED.getMessage();
    /** error 日志中 body 最大保留长度，避免整段 JSON 落盘 */
    private static final int LOG_BODY_MAX_LEN = 512;

    private final AiProperties aiProperties;

    /**
     * 同步调用百炼应用 completion
     */
    public ChatResultDTO completion(String appId, String apiKey, ChatRequestDTO request) {
        String url = buildUrl(appId);
        String body = buildRequestBody(request, false);

        String responseBody;
        int statusCode;
        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .setConnectionTimeout(aiProperties.getConnectTimeoutMs())
                    .setReadTimeout(aiProperties.getReadTimeoutMs())
                    .execute();
            statusCode = response.getStatus();
            responseBody = response.body();
        } catch (Exception e) {
            log.error("百炼应用调用失败: appId={}", appId, e);
            throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
        }

        if (statusCode < 200 || statusCode >= 300) {
            logUpstreamFailure("百炼应用返回非成功状态", appId, statusCode, responseBody);
            throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
        }

        return parseResponse(responseBody);
    }

    /**
     * SSE 流式调用百炼应用 completion，逐块回调。
     * <p>
     * 必须使用 {@code executeAsync()}：同步 {@code execute()} 会把整段响应读入内存后再返回，
     * <p>
     */
    public void completionStream(String appId, String apiKey, ChatRequestDTO request,
                                 Consumer<ChatStreamEventDTO> onEvent) {
        String url = buildUrl(appId);
        String body = buildRequestBody(request, true);

        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-SSE", "enable")
                .header("Accept", "text/event-stream")
                .body(body)
                .setConnectionTimeout(aiProperties.getConnectTimeoutMs())
                .setReadTimeout(aiProperties.getStreamReadTimeoutMs())
                .executeAsync()) {

            int statusCode = response.getStatus();
            if (statusCode < 200 || statusCode >= 300) {
                String errBody = response.body();
                logUpstreamFailure("百炼流式返回失败", appId, statusCode, errBody);
                throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.bodyStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StrUtil.isBlank(line) || line.startsWith(":") || line.startsWith("id:")
                            || line.startsWith("event:") || line.startsWith("retry:")) {
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (StrUtil.isBlank(data) || "[DONE]".equalsIgnoreCase(data)) {
                        continue;
                    }
                    ChatStreamEventDTO event = parseStreamData(data);
                    if (event != null) {
                        onEvent.accept(event);
                    }
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("百炼流式调用/读取失败: appId={}", appId, e);
            throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
        }
    }

    String buildUrl(String appId) {
        String base = StrUtil.removeSuffix(aiProperties.getBaseUrl(), "/");
        return base + "/apps/" + appId + "/completion";
    }

    String buildRequestBody(ChatRequestDTO request, boolean stream) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", request.getPrompt());
        if (StrUtil.isNotBlank(request.getSessionId())) {
            input.put("session_id", request.getSessionId());
        }
        appendAttachmentLists(request.getAttachments(), input);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("input", input);

        Map<String, Object> parameters = new LinkedHashMap<>();
        boolean enableThinking = Boolean.TRUE.equals(request.getEnableThinking());
        boolean hasThoughts = Boolean.TRUE.equals(request.getHasThoughts());
        if (enableThinking) {
            parameters.put("enable_thinking", true);
        }
        if (hasThoughts) {
            parameters.put("has_thoughts", true);
        }
        if (stream) {
            parameters.put("stream", true);
            parameters.put("incremental_output", true);
        }
        if (!parameters.isEmpty()) {
            payload.put("parameters", parameters);
        }

        return JSONUtil.toJsonStr(payload);
    }

    /**
     * 百炼 input：图片 URL 进 image_list，其它文件进 file_list
     */
    void appendAttachmentLists(List<ChatAttachmentDTO> attachments, Map<String, Object> input) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        List<String> imageList = new ArrayList<>();
        List<String> fileList = new ArrayList<>();
        for (ChatAttachmentDTO att : attachments) {
            if (att == null || StrUtil.isBlank(att.getUrl())) {
                continue;
            }
            String url = att.getUrl().trim();
            if (isImageAttachment(att)) {
                imageList.add(url);
            } else {
                fileList.add(url);
            }
        }
        if (!imageList.isEmpty()) {
            input.put("image_list", imageList);
        }
        if (!fileList.isEmpty()) {
            input.put("file_list", fileList);
        }
    }

    boolean isImageAttachment(ChatAttachmentDTO att) {
        if (att == null) {
            return false;
        }
        String contentType = StrUtil.trim(att.getContentType());
        if (StrUtil.isNotBlank(contentType) && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        String name = StrUtil.trim(att.getName());
        if (StrUtil.isBlank(name)) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    ChatResultDTO parseResponse(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
        }

        JSONObject root = JSONUtil.parseObj(responseBody);
        if (isBusinessError(root)) {
            logUpstreamFailure("百炼应用业务错误", null, null, responseBody);
            throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
        }

        JSONObject output = root.getJSONObject("output");
        if (output == null) {
            logUpstreamFailure("百炼应用响应缺少 output", null, null, responseBody);
            throw new BizException(ResultCode.AI_CALL_FAILED, CLIENT_ERROR_MESSAGE);
        }

        Integer[] tokens = extractTokens(root);
        return ChatResultDTO.builder()
                .text(output.getStr("text"))
                .thoughts(extractThoughts(output))
                .docReferences(extractDocReferences(output))
                .sessionId(output.getStr("session_id"))
                .finishReason(normalizeFinishReason(output.getStr("finish_reason")))
                .requestId(root.getStr("request_id"))
                .inputTokens(tokens[0])
                .outputTokens(tokens[1])
                .build();
    }

    /**
     * 解析单条 SSE data JSON，转为前端事件
     */
    ChatStreamEventDTO parseStreamData(String dataJson) {
        JSONObject root = JSONUtil.parseObj(dataJson);
        if (isBusinessError(root)) {
            logUpstreamFailure("百炼流式业务错误", null, null, dataJson);
            return ChatStreamEventDTO.builder()
                    .type("error")
                    .message(CLIENT_ERROR_MESSAGE)
                    .requestId(root.getStr("request_id"))
                    .build();
        }

        JSONObject output = root.getJSONObject("output");
        if (output == null) {
            return null;
        }

        String finishReason = normalizeFinishReason(output.getStr("finish_reason"));
        boolean done = "stop".equalsIgnoreCase(finishReason);
        Integer[] tokens = extractTokens(root);

        return ChatStreamEventDTO.builder()
                .type(done ? "done" : "delta")
                .text(output.getStr("text"))
                .thoughts(extractThoughts(output))
                .docReferences(extractDocReferences(output))
                .sessionId(output.getStr("session_id"))
                .finishReason(finishReason)
                .requestId(root.getStr("request_id"))
                .inputTokens(tokens[0])
                .outputTokens(tokens[1])
                .build();
    }

    /**
     * 百炼进行中时常返回字面量 "null"，对外统一为 null。
     */
    String normalizeFinishReason(String finishReason) {
        if (StrUtil.isBlank(finishReason) || "null".equalsIgnoreCase(finishReason)) {
            return null;
        }
        return finishReason;
    }

    List<ThoughtItemDTO> extractThoughts(JSONObject output) {
        if (output == null) {
            return null;
        }
        JSONArray thoughts = output.getJSONArray("thoughts");
        if (thoughts == null || thoughts.isEmpty()) {
            return null;
        }
        List<ThoughtItemDTO> list = new ArrayList<>(thoughts.size());
        for (int i = 0; i < thoughts.size(); i++) {
            JSONObject item = thoughts.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(ThoughtItemDTO.builder()
                    .action(item.getStr("action"))
                    .thought(item.getStr("thought"))
                    .actionType(item.getStr("action_type"))
                    .response(item.getStr("response"))
                    .actionName(item.getStr("action_name"))
                    .build());
        }
        return list.isEmpty() ? null : list;
    }

    List<DocReferenceDTO> extractDocReferences(JSONObject output) {
        if (output == null) {
            return null;
        }
        JSONArray references = output.getJSONArray("doc_references");
        if (references == null || references.isEmpty()) {
            return null;
        }
        List<DocReferenceDTO> list = new ArrayList<>(references.size());
        for (int i = 0; i < references.size(); i++) {
            JSONObject item = references.getJSONObject(i);
            if (item == null) {
                continue;
            }
            JSONArray imageArray = item.getJSONArray("images");
            List<String> images = imageArray == null
                    ? null
                    : imageArray.stream().map(String::valueOf).toList();
            list.add(DocReferenceDTO.builder()
                    .indexId(item.getStr("index_id"))
                    .title(item.getStr("title"))
                    .docId(item.getStr("doc_id"))
                    .docName(item.getStr("doc_name"))
                    .docUrl(StrUtil.blankToDefault(
                            item.getStr("doc_url"),
                            StrUtil.blankToDefault(item.getStr("url"), item.getStr("source_url"))))
                    .text(item.getStr("text"))
                    .bizId(item.getStr("biz_id"))
                    .images(images)
                    .build());
        }
        return list.isEmpty() ? null : list;
    }

    private boolean isBusinessError(JSONObject root) {
        return root.containsKey("code") && StrUtil.isNotBlank(root.getStr("code"))
                && !"Success".equalsIgnoreCase(root.getStr("code"));
    }

    private Integer[] extractTokens(JSONObject root) {
        Integer inputTokens = null;
        Integer outputTokens = null;
        JSONObject usage = root.getJSONObject("usage");
        if (usage != null) {
            JSONArray models = usage.getJSONArray("models");
            if (models != null && !models.isEmpty()) {
                JSONObject first = models.getJSONObject(0);
                if (first != null) {
                    inputTokens = first.getInt("input_tokens");
                    outputTokens = first.getInt("output_tokens");
                }
            }
        }
        return new Integer[]{inputTokens, outputTokens};
    }

    /**
     * 服务端日志：记录可排查字段 + 截断 body；不向调用方返回上游原文。
     */
    private void logUpstreamFailure(String scene, String appId, Integer status, String responseBody) {
        String code = null;
        String requestId = null;
        String upstreamMessage = null;
        if (StrUtil.isNotBlank(responseBody)) {
            try {
                JSONObject root = JSONUtil.parseObj(responseBody);
                code = root.getStr("code");
                requestId = root.getStr("request_id");
                upstreamMessage = root.getStr("message");
                if (StrUtil.isBlank(upstreamMessage)) {
                    upstreamMessage = root.getStr("msg");
                }
            } catch (Exception ignored) {
                // body 非 JSON 时仅打截断原文
            }
        }
        log.error("{}: appId={}, status={}, code={}, requestId={}, upstreamMessage={}, body={}",
                scene,
                appId,
                status,
                code,
                requestId,
                truncateForLog(upstreamMessage),
                truncateForLog(responseBody));
    }

    String truncateForLog(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= LOG_BODY_MAX_LEN) {
            return value;
        }
        return value.substring(0, LOG_BODY_MAX_LEN) + "...(truncated,len=" + value.length() + ")";
    }
}
