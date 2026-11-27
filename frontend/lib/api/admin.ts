import { api } from "./client";
import type { AdminAuditLogDTO, AdminConversationSessionDTO, ApiBehaviorLogDTO, ConversationMessageDTO, OperationsOverviewDTO, PageResult, SecondFactorTokenDTO } from "@/types/api";

export const adminApi = {
  sendSecondFactorCode: () => api.post<string>("/admin/second-factor/code"),
  verifySecondFactor: (code: string) =>
    api.post<SecondFactorTokenDTO>("/admin/second-factor/verify", { code }),
  overview: () => api.get<OperationsOverviewDTO>("/admin/operations/overview"),
  auditLogs: (current = 1, size = 20) => api.get<PageResult<AdminAuditLogDTO>>("/admin/audit-logs", { query: { current, size } }),
  behaviorLogs: (current = 1, size = 20) => api.get<PageResult<ApiBehaviorLogDTO>>("/admin/behavior-logs", { query: { current, size } }),
  conversations: (current = 1, size = 10, keyword?: string) =>
    api.get<PageResult<AdminConversationSessionDTO>>("/admin/conversations", {
      query: { current, size, keyword },
    }),
  conversationMessages: (sessionId: string) =>
    api.get<ConversationMessageDTO[]>(
      `/admin/conversations/${encodeURIComponent(sessionId)}/messages`,
    ),
  exportAuditLogs: async (token: string) => {
    return api.download("/admin/audit-logs/export", { headers: { "X-Admin-Confirm-Token": token } });
  },
  exportBehaviorLogs: async (token: string) => {
    return api.download("/admin/behavior-logs/export", { headers: { "X-Admin-Confirm-Token": token } });
  },
};
