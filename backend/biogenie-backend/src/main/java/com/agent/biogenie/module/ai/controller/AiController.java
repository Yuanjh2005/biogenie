package com.agent.biogenie.module.ai.controller;

import com.agent.biogenie.common.PageResult;
import com.agent.biogenie.common.Result;
import com.agent.biogenie.common.auth.AuthContext;
import com.agent.biogenie.config.AiProperties;
import com.agent.biogenie.dto.ai.ChatRequestDTO;
import com.agent.biogenie.dto.ai.ChatResultDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.ai.ConversationSessionDTO;
import com.agent.biogenie.dto.ai.HotQuestionDTO;
import com.agent.biogenie.module.ai.service.AiService;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.agent.biogenie.module.ai.service.HotQuestionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 对话接口（阿里云百炼多轮对话代理 + 会话管理）
 */
@Tag(name = "AI 对话", description = "阿里云百炼应用多轮对话与会话历史")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final ConversationService conversationService;
    private final HotQuestionService hotQuestionService;
    private final AiProperties aiProperties;

    @Operation(summary = "多轮对话(已废弃)")
    @PostMapping("/chat")
    public Result<ChatResultDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        return Result.ok(aiService.chat(request));
    }

    @Operation(summary = "多轮对话（SSE 流式）",
            description = "请求体与同步接口相同；响应为 text/event-stream。"
                    + "事件名: delta（增量文本）/ done（结束，含 sessionId）/ error。"
                    + "仅 done 成功后落库。")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO request,
                                 HttpServletResponse response) {
        // 避免 Nginx/代理缓冲整段 SSE，导致前端无法边收边显
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        SseEmitter emitter = new SseEmitter((long) aiProperties.getStreamTimeoutMs());
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> {
            // 客户端断开等，忽略即可
        });
        aiService.chatStream(request, emitter);
        return emitter;
    }

    @Operation(summary = "查询热门问题 TopN",
            description = "先读 Redis；不足则从 MySQL 补全并回填。默认 3 条，最大 10。")
    @GetMapping("/hot-questions")
    public Result<List<HotQuestionDTO>> listHotQuestions(
            @Parameter(description = "条数，默认 3", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) int limit,
            @Parameter(description = "应用配置名，不传则为 global", example = "smartdrgf-fast")
            @RequestParam(required = false) String app) {
        return Result.ok(hotQuestionService.listTop(app, limit));
    }

    @Operation(summary = "分页查询当前用户会话列表")
    @GetMapping("/conversations")
    public Result<PageResult<ConversationSessionDTO>> listConversations(
            @Parameter(description = "页码", example = "1")
            @RequestParam(name = "current", defaultValue = "1") @Min(1) int current,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(10) int size,
            @Parameter(description = "按会话标题模糊搜索", example = "EGFR")
            @RequestParam(required = false) String keyword) {
        Page<ConversationSessionDTO> page =
                conversationService.pageSessions(AuthContext.getUserId(), current, size, keyword);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @Operation(summary = "查询会话历史消息")
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<ConversationMessageDTO>> listMessages(
            @PathVariable String sessionId) {
        return Result.ok(conversationService.listMessages(AuthContext.getUserId(), sessionId));
    }

    @Operation(summary = "归档会话")
    @PostMapping("/conversations/{sessionId}/archive")
    public Result<String> archive(@PathVariable String sessionId) {
        conversationService.archive(AuthContext.getUserId(), sessionId);
        return Result.ok("归档成功");
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/conversations/{sessionId}")
    public Result<String> delete(@PathVariable String sessionId) {
        conversationService.delete(AuthContext.getUserId(), sessionId);
        return Result.ok("删除成功");
    }

}
