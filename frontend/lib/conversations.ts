import { moduleFromAppKey } from "@/lib/config/agents";
import { SHELL_COPY } from "@/lib/i18n/shell";
import { useLocaleStore } from "@/stores/locale";
import type { ConversationMessageDTO, ConversationSessionDTO } from "@/types/api";
import type { ChatMessage, Conversation, ConversationGroups } from "@/types/chat";

/** 按 今天 / 昨天 / 更早 分组会话（依据 updatedAt） */
export function groupConversations(list: Conversation[]): ConversationGroups {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfYesterday = startOfToday - 24 * 60 * 60 * 1000;

  const groups: ConversationGroups = { today: [], yesterday: [], earlier: [] };
  const sorted = [...list].sort((a, b) => b.updatedAt - a.updatedAt);

  for (const c of sorted) {
    if (c.updatedAt >= startOfToday) groups.today.push(c);
    else if (c.updatedAt >= startOfYesterday) groups.yesterday.push(c);
    else groups.earlier.push(c);
  }
  return groups;
}

export function conversationFromSession(
  dto: ConversationSessionDTO,
): Conversation {
  const moduleKey = moduleFromAppKey(dto.appKey);
  const updatedAt = dto.updateTime
    ? new Date(dto.updateTime).getTime()
    : dto.createTime
      ? new Date(dto.createTime).getTime()
      : Date.now();
  return {
    id: dto.sessionId,
    module: moduleKey,
    title: dto.title || SHELL_COPY[useLocaleStore.getState().locale].untitledConversation,
    sessionId: dto.sessionId,
    updatedAt: Number.isFinite(updatedAt) ? updatedAt : Date.now(),
  };
}

/** 后端一问一答 → 前端消息列表 */
export function messagesFromTurns(
  turns: ConversationMessageDTO[],
): ChatMessage[] {
  const msgs: ChatMessage[] = [];
  for (const t of turns) {
    const createdAt = t.createTime
      ? new Date(t.createTime).getTime()
      : Date.now();
    const ts = Number.isFinite(createdAt) ? createdAt : Date.now();
    const attachments = (t.attachments ?? [])
      .filter((a) => a.url || a.name)
      .map((a, i) => ({
        id: `att-${t.id}-${i}`,
        name: a.name || SHELL_COPY[useLocaleStore.getState().locale].attachment,
        size: 0,
        type: a.contentType || "",
        url: a.url,
        status: "done" as const,
      }));
    msgs.push({
      id: `q-${t.id}`,
      role: "user",
      content: t.question || "",
      status: "done",
      ...(attachments.length ? { attachments } : {}),
      createdAt: ts,
    });
    msgs.push({
      id: `a-${t.id}`,
      role: "assistant",
      content: t.answer || "",
      status: "done",
      finishReason: t.finishReason,
      createdAt: ts + 1,
    });
  }
  return msgs;
}
