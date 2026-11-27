package com.agent.biogenie.module.admin.controller;

import com.agent.biogenie.common.PageResult;
import com.agent.biogenie.common.Result;
import com.agent.biogenie.common.auth.AuthContext;
import com.agent.biogenie.common.auth.RequiredAdmin;
import com.agent.biogenie.common.auth.RequiredSuperAdmin;
import com.agent.biogenie.dto.admin.OperationsOverviewDTO;
import com.agent.biogenie.dto.admin.AdminConversationSessionDTO;
import com.agent.biogenie.dto.ai.ConversationMessageDTO;
import com.agent.biogenie.dto.user.SecondFactorTokenDTO;
import com.agent.biogenie.dto.user.SecondFactorVerifyDTO;
import com.agent.biogenie.module.admin.entity.AdminAuditLog;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import com.agent.biogenie.module.admin.service.AdminAuditService;
import com.agent.biogenie.module.admin.service.AdminSecondFactorService;
import com.agent.biogenie.module.admin.service.OperationsStatisticsService;
import com.agent.biogenie.module.admin.service.ApiBehaviorLogService;
import com.agent.biogenie.module.admin.service.LogExcelExportService;
import com.agent.biogenie.module.ai.service.ConversationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理中心")
@RestController
@RequestMapping("/api/admin")
@RequiredAdmin
@RequiredArgsConstructor
public class AdminController {
    private final AdminSecondFactorService secondFactorService;
    private final OperationsStatisticsService statisticsService;
    private final AdminAuditService auditService;
    private final ApiBehaviorLogService behaviorLogService;
    private final LogExcelExportService excelExportService;
    private final ConversationService conversationService;

    @Operation(summary = "发送管理员二次确认验证码")
    @PostMapping("/second-factor/code")
    public Result<String> sendSecondFactorCode() {
        String maskedPhone = secondFactorService.sendCode(AuthContext.getUserId());
        return Result.ok("验证码已发送至 " + maskedPhone);
    }

    @Operation(summary = "校验验证码并换取一次性高危操作令牌")
    @PostMapping("/second-factor/verify")
    public Result<SecondFactorTokenDTO> verifySecondFactor(@Valid @RequestBody SecondFactorVerifyDTO dto) {
        return Result.ok(secondFactorService.verify(AuthContext.getUserId(), AuthContext.getSessionId(), dto.getCode()));
    }

    @RequiredSuperAdmin
    @Operation(summary = "运营数据总览")
    @GetMapping("/operations/overview")
    public Result<OperationsOverviewDTO> overview() {
        return Result.ok(statisticsService.overview());
    }

    @RequiredSuperAdmin
    @Operation(summary = "分页查询管理员操作审计")
    @GetMapping("/audit-logs")
    public Result<PageResult<AdminAuditLog>> auditLogs(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(20) int size,
            @RequestParam(required = false) String action) {
        Page<AdminAuditLog> page = auditService.page(current, size, action);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @RequiredSuperAdmin
    @Operation(summary = "导出管理员操作审计 Excel")
    @GetMapping("/audit-logs/export")
    public void exportAuditLogs(
            @RequestHeader("X-Admin-Confirm-Token") String confirmToken,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse response) throws IOException {
        secondFactorService.consume(AuthContext.getUserId(), AuthContext.getSessionId(), confirmToken);
        auditService.record(AuthContext.getUserId(), "EXPORT_ADMIN_AUDIT", "LOG", "ADMIN_AUDIT",
                "action=" + action + ",from=" + from + ",to=" + to);
        excelExportService.exportAudit(auditService.listForExport(action, from, to), response);
    }

    @RequiredSuperAdmin
    @Operation(summary = "分页查询用户及管理员 API 调用行为")
    @GetMapping("/behavior-logs")
    public Result<PageResult<ApiBehaviorLog>> behaviorLogs(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(20) int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Page<ApiBehaviorLog> page = behaviorLogService.page(current, size, userId, role, from, to);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @Operation(summary = "分页查询所有用户 AI 会话")
    @GetMapping("/conversations")
    public Result<PageResult<AdminConversationSessionDTO>> conversations(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size,
            @RequestParam(required = false) String keyword) {
        Page<AdminConversationSessionDTO> page =
                conversationService.pageAllSessions(current, size, keyword);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @Operation(summary = "查看指定用户 AI 会话消息")
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<ConversationMessageDTO>> conversationMessages(@PathVariable String sessionId) {
        return Result.ok(conversationService.listAllMessages(sessionId));
    }

    @RequiredSuperAdmin
    @Operation(summary = "导出用户及管理员 API 调用行为 Excel")
    @GetMapping("/behavior-logs/export")
    public void exportBehaviorLogs(
            @RequestHeader("X-Admin-Confirm-Token") String confirmToken,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse response) throws IOException {
        secondFactorService.consume(AuthContext.getUserId(), AuthContext.getSessionId(), confirmToken);
        auditService.record(AuthContext.getUserId(), "EXPORT_API_BEHAVIOR", "LOG", "API_BEHAVIOR",
                "userId=" + userId + ",role=" + role + ",from=" + from + ",to=" + to);
        excelExportService.exportBehavior(behaviorLogService.listForExport(userId, role, from, to), response);
    }
}
