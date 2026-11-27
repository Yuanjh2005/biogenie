package com.agent.biogenie.module.ai.controller;

import com.agent.biogenie.common.Result;
import com.agent.biogenie.common.auth.Anonymous;
import com.agent.biogenie.common.auth.AuthContext;
import com.agent.biogenie.dto.ai.ConversationShareCreateDTO;
import com.agent.biogenie.dto.ai.ConversationShareViewDTO;
import com.agent.biogenie.module.ai.service.ConversationShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 对话分享（整段快照，固定 12h）
 */
@Tag(name = "AI 对话分享", description = "整段会话分享链接：创建与匿名打开")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ConversationShareController {

    private final ConversationShareService conversationShareService;

    @Operation(summary = "创建整段会话分享链接",
            description = "将当前会话全部消息固化为快照，生成 12 小时有效的只读分享链接。")
    @PostMapping("/conversations/{sessionId}/share")
    public Result<ConversationShareCreateDTO> create(@PathVariable String sessionId) {
        return Result.ok(conversationShareService.create(AuthContext.getUserId(), sessionId));
    }

    @Anonymous
    @Operation(summary = "打开分享（匿名）",
            description = "按 token 读取快照；已过期则将 status 置 0 并返回过期错误。")
    @GetMapping("/shares/{shareToken}")
    public Result<ConversationShareViewDTO> getByToken(@PathVariable String shareToken) {
        return Result.ok(conversationShareService.getByToken(shareToken));
    }
}
