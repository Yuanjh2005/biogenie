import { api } from "./client";
import type { PageResult } from "@/types/api";
import type {
  ConversationMessageDTO,
  ConversationSessionDTO,
  ConversationShareCreateDTO,
  ConversationShareViewDTO,
} from "@/types/api";

export const conversationApi = {
  list: (current = 1, size = 10, keyword?: string) =>
    api.get<PageResult<ConversationSessionDTO>>("/ai/conversations", {
      query: { current, size, ...(keyword ? { keyword } : {}) },
    }),

  messages: (sessionId: string) =>
    api.get<ConversationMessageDTO[]>(
      `/ai/conversations/${encodeURIComponent(sessionId)}/messages`,
    ),

  archive: (sessionId: string) =>
    api.post<string>(
      `/ai/conversations/${encodeURIComponent(sessionId)}/archive`,
    ),

  remove: (sessionId: string) =>
    api.delete<string>(
      `/ai/conversations/${encodeURIComponent(sessionId)}`,
    ),

  /** 创建整段会话分享（12h 有效） */
  share: (sessionId: string) =>
    api.post<ConversationShareCreateDTO>(
      `/ai/conversations/${encodeURIComponent(sessionId)}/share`,
    ),

  /** 匿名打开分享快照 */
  getShare: (shareToken: string) =>
    api.get<ConversationShareViewDTO>(
      `/ai/shares/${encodeURIComponent(shareToken)}`,
      { auth: false },
    ),
};
