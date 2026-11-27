import { create } from "zustand";
import { streamChat } from "@/lib/api/chat";
import { conversationApi } from "@/lib/api/conversations";
import { mockCreateConversation } from "@/lib/api/mock";
import {
  conversationFromSession,
  messagesFromTurns,
} from "@/lib/conversations";
import { env } from "@/lib/config/env";
import type {
  Attachment,
  ChatMessage,
  ChatMode,
  Conversation,
  ModuleKey,
} from "@/types/chat";
import { countWebPages } from "@/lib/chat/web-pages";
import { appendStreamDelta } from "@/lib/chat/stream-text";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import { ApiError } from "@/lib/api/errors";

function uid(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function chatCopy() {
  return CHAT_COPY[useLocaleStore.getState().locale];
}

function isRetrievingWeb(text: string | undefined): boolean {
  return text === CHAT_COPY["zh-CN"].retrievingWeb || text === CHAT_COPY.en.retrievingWeb;
}

interface SendOptions {
  attachments?: Attachment[];
  redesignOption?: string;
  pubmedYear?: string;
  pubmedArticleType?: string;
  replaceMessageId?: string;
}

interface ChatState {
  conversations: Conversation[];
  messagesByConversation: Record<string, ChatMessage[]>;
  activeIdByModule: Partial<Record<ModuleKey, string>>;
  modeByModule: Record<ModuleKey, ChatMode>;
  streamingConversationIds: Record<string, true>;
  abortControllers: Record<string, AbortController>;
  conversationsLoaded: boolean;
  conversationsLoading: boolean;
  conversationsLoadFailed: boolean;
  conversationsPage: number;
  conversationsTotal: number;
  conversationPageIds: string[];
  conversationsKeyword: string;
  messagesLoadingId: string | null;

  setMode: (module: ModuleKey, mode: ChatMode) => void;
  getActiveConversation: (module: ModuleKey) => Conversation | undefined;
  getMessages: (conversationId: string | undefined) => ChatMessage[];

  loadConversations: (page?: number, keyword?: string) => Promise<void>;
  reset: () => void;
  newConversation: (module: ModuleKey) => void;
  selectConversation: (id: string) => Promise<void>;
  deleteConversation: (id: string) => Promise<void>;

  sendMessage: (module: ModuleKey, prompt: string, opts?: SendOptions) => Promise<void>;
  stopGenerating: (conversationId?: string) => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  messagesByConversation: {},
  activeIdByModule: {},
  modeByModule: {
    "smart-drgf": "fast",
    "deep-pubmed": "fast",
    "design-mcp": "fast",
  },
  streamingConversationIds: {},
  abortControllers: {},
  conversationsLoaded: false,
  conversationsLoading: false,
  conversationsLoadFailed: false,
  conversationsPage: 1,
  conversationsTotal: 0,
  conversationPageIds: [],
  conversationsKeyword: "",
  messagesLoadingId: null,

  setMode: (module, mode) =>
    set((s) => ({ modeByModule: { ...s.modeByModule, [module]: mode } })),

  getActiveConversation: (module) => {
    const id = get().activeIdByModule[module];
    return get().conversations.find((c) => c.id === id);
  },

  getMessages: (conversationId) =>
    conversationId ? get().messagesByConversation[conversationId] ?? [] : [],

  loadConversations: async (requestedPage = 1, requestedKeyword = get().conversationsKeyword) => {
    const keyword = requestedKeyword.trim();
    if (env.useMockChat) {
      const matches = get().conversations.filter((conversation) =>
        conversation.title.toLocaleLowerCase().includes(keyword.toLocaleLowerCase()),
      );
      set({
        conversationsLoaded: true,
        conversationsLoading: false,
        conversationsLoadFailed: false,
        conversationsPage: 1,
        conversationsTotal: matches.length,
        conversationPageIds: matches.map((conversation) => conversation.id),
        conversationsKeyword: keyword,
      });
      return;
    }
    if (get().conversationsLoading) return;
    set({ conversationsLoading: true, conversationsLoadFailed: false });
    try {
      let page;
      try {
        page = await conversationApi.list(requestedPage, 10, keyword);
      } catch (error) {
        const retryable =
          !(error instanceof ApiError) || error.status === 0 || error.status >= 500;
        if (!retryable) throw error;
        await new Promise((resolve) => window.setTimeout(resolve, 500));
        page = await conversationApi.list(requestedPage, 10, keyword);
      }
      const list = (page.records ?? [])
        .map(conversationFromSession)
        .filter((c): c is Conversation => Boolean(c));
      set((s) => {
        if (requestedPage > 1) {
          const existingIds = new Set(s.conversations.map((conversation) => conversation.id));
          const loadedIds = new Set(s.conversationPageIds);
          return {
            conversations: [
              ...s.conversations,
              ...list.filter((conversation) => !existingIds.has(conversation.id)),
            ],
            conversationsLoaded: true,
            conversationsLoading: false,
            conversationsLoadFailed: false,
            conversationsPage: requestedPage,
            conversationsTotal: page.total,
            conversationsKeyword: keyword,
            conversationPageIds: [
              ...s.conversationPageIds,
              ...list
                .map((conversation) => conversation.id)
                .filter((id) => !loadedIds.has(id)),
            ],
          };
        }

        const serverIds = new Set(list.map((c) => c.id));
        const localKeep = s.conversations.filter((c) => {
          if (serverIds.has(c.id)) return false;
          if (s.streamingConversationIds[c.id]) return true;
          if (c.sessionId && s.streamingConversationIds[c.sessionId]) return true;
          if (!c.sessionId) return true;
          if (s.activeIdByModule[c.module] === c.id) return true;
          return false;
        });
        return {
          conversations: [...localKeep, ...list],
          conversationsLoaded: true,
          conversationsLoading: false,
          conversationsLoadFailed: false,
          conversationsPage: requestedPage,
          conversationsTotal: page.total,
          conversationsKeyword: keyword,
          conversationPageIds: list.map((conversation) => conversation.id),
        };
      });
    } catch {
      // 保留已有列表，同时暴露失败状态，允许用户主动重试。
      set({
        conversationsLoaded: true,
        conversationsLoading: false,
        conversationsLoadFailed: true,
      });
    }
  },

  reset: () => {
    Object.values(get().abortControllers).forEach((controller) => controller.abort());
    set({
      conversations: [],
      messagesByConversation: {},
      activeIdByModule: {},
      streamingConversationIds: {},
      abortControllers: {},
      conversationsLoaded: false,
      conversationsLoading: false,
      conversationsLoadFailed: false,
      conversationsPage: 1,
      conversationsTotal: 0,
      conversationPageIds: [],
      conversationsKeyword: "",
      messagesLoadingId: null,
    });
  },

  newConversation: (module) =>
    set((s) => ({ activeIdByModule: { ...s.activeIdByModule, [module]: undefined } })),

  selectConversation: async (id) => {
    const conv = get().conversations.find((c) => c.id === id);
    if (!conv) return;
    set((s) => ({ activeIdByModule: { ...s.activeIdByModule, [conv.module]: id } }));

    if (env.useMockChat) return;
    if (get().messagesByConversation[id]?.length) return;
    if (!conv.sessionId) return;

    set({ messagesLoadingId: id });
    try {
      const turns = await conversationApi.messages(conv.sessionId);
      set((s) => ({
        messagesByConversation: {
          ...s.messagesByConversation,
          [id]: messagesFromTurns(turns),
        },
        messagesLoadingId: s.messagesLoadingId === id ? null : s.messagesLoadingId,
      }));
    } catch {
      set((s) => ({
        messagesLoadingId: s.messagesLoadingId === id ? null : s.messagesLoadingId,
      }));
    }
  },

  deleteConversation: async (id) => {
    const conv = get().conversations.find((c) => c.id === id);
    get().abortControllers[id]?.abort();
    if (!env.useMockChat && conv?.sessionId) {
      try {
        await conversationApi.remove(conv.sessionId);
      } catch {
        // 后端删除失败时仍清理本地，避免卡死
      }
    }

    set((s) => {
      const rest = { ...s.messagesByConversation };
      delete rest[id];
      const activeIdByModule = { ...s.activeIdByModule };
      if (conv && activeIdByModule[conv.module] === id) {
        activeIdByModule[conv.module] = undefined;
      }
      return {
        conversations: s.conversations.filter((c) => c.id !== id),
        conversationPageIds: s.conversationPageIds.filter((conversationId) => conversationId !== id),
        conversationsTotal: conv?.sessionId
          ? Math.max(0, s.conversationsTotal - 1)
          : s.conversationsTotal,
        messagesByConversation: rest,
        activeIdByModule,
        streamingConversationIds: Object.fromEntries(
          Object.entries(s.streamingConversationIds).filter(([conversationId]) => conversationId !== id),
        ),
        abortControllers: Object.fromEntries(
          Object.entries(s.abortControllers).filter(([conversationId]) => conversationId !== id),
        ),
      };
    });
  },

  sendMessage: async (module, prompt, opts) => {
    const text = prompt.trim();
    if (!text) return;

    const mode = get().modeByModule[module];
    const requestStartedAt = Date.now();

    let conversation = get().getActiveConversation(module);
    if (!conversation) {
      conversation = env.useMockChat
        ? mockCreateConversation(module, text.slice(0, 20))
        : {
            id: uid("pending"),
            module,
            title: text.slice(0, 20),
            updatedAt: requestStartedAt,
          };
      set((s) => ({
        conversations: [conversation!, ...s.conversations],
        activeIdByModule: { ...s.activeIdByModule, [module]: conversation!.id },
      }));
    }
    let convId = conversation.id;
    if (get().streamingConversationIds[convId]) return;

    // 一次发送只提升一次会话顺序。流式分片会交错到达，不能在每个分片上
    // 重写 updatedAt，否则并发会话会不断争抢列表首位并产生闪烁。
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === convId ? { ...c, updatedAt: requestStartedAt } : c,
      ),
    }));

    const currentMessages = get().messagesByConversation[convId] ?? [];
    const replaceIndex = opts?.replaceMessageId
      ? currentMessages.findIndex(
          (message) => message.id === opts.replaceMessageId && message.role === "user",
        )
      : -1;
    const previousAssistant =
      replaceIndex >= 0 && currentMessages[replaceIndex + 1]?.role === "assistant"
        ? currentMessages[replaceIndex + 1]
        : undefined;

    const userMsg: ChatMessage = {
      id: replaceIndex >= 0 ? currentMessages[replaceIndex].id : uid("msg"),
      role: "user",
      content: text,
      status: "done",
      attachments: opts?.attachments,
      createdAt: Date.now(),
    };
    const assistantMsg: ChatMessage = {
      id: previousAssistant?.id ?? uid("msg"),
      role: "assistant",
      content: "",
      status: "pending",
      mode,
      retrievalSummary: mode === "web" ? chatCopy().retrievingWeb : undefined,
      createdAt: Date.now(),
    };

    set((s) => ({
      messagesByConversation: {
        ...s.messagesByConversation,
        [convId]:
          replaceIndex >= 0
            ? [
                ...(s.messagesByConversation[convId] ?? []).slice(0, replaceIndex),
                userMsg,
                assistantMsg,
              ]
            : [...(s.messagesByConversation[convId] ?? []), userMsg, assistantMsg],
      },
    }));

    const controller = new AbortController();
    set((s) => ({
      streamingConversationIds: { ...s.streamingConversationIds, [convId]: true },
      abortControllers: { ...s.abortControllers, [convId]: controller },
    }));

    const patchAssistant = (patch: Partial<ChatMessage>, targetId = convId) =>
      set((s) => ({
        messagesByConversation: {
          ...s.messagesByConversation,
          [targetId]: (s.messagesByConversation[targetId] ?? []).map((m) =>
            m.id === assistantMsg.id ? { ...m, ...patch } : m,
          ),
        },
      }));

    const migrateToSessionId = (sessionId: string) => {
      if (!sessionId || sessionId === convId) return;
      const oldId = convId;
      set((s) => {
        const msgs =
          s.messagesByConversation[oldId] ?? s.messagesByConversation[sessionId] ?? [];
        const rest = { ...s.messagesByConversation };
        delete rest[oldId];
        rest[sessionId] = msgs;
        const existing = s.conversations.find((c) => c.id === oldId || c.id === sessionId);
        const nextConv: Conversation = existing
          ? { ...existing, id: sessionId, sessionId }
          : {
              id: sessionId,
              sessionId,
              module,
              title: text.slice(0, 20),
              updatedAt: requestStartedAt,
            };
        const conversations = existing
          ? s.conversations.map((c) =>
              c.id === oldId || c.id === sessionId ? nextConv : c,
            )
          : [nextConv, ...s.conversations];
        const streamingConversationIds = { ...s.streamingConversationIds };
        const abortControllers = { ...s.abortControllers };
        if (streamingConversationIds[oldId]) {
          delete streamingConversationIds[oldId];
          streamingConversationIds[sessionId] = true;
        }
        if (abortControllers[oldId] === controller) {
          delete abortControllers[oldId];
          abortControllers[sessionId] = controller;
        }
        return {
          conversations,
          conversationPageIds: s.conversationPageIds.includes(oldId)
            ? s.conversationPageIds.map((id) => id === oldId ? sessionId : id)
            : [sessionId, ...s.conversationPageIds],
          conversationsTotal:
            s.conversationPageIds.includes(oldId) || s.conversationPageIds.includes(sessionId)
              ? s.conversationsTotal
              : s.conversationsTotal + 1,
          messagesByConversation: rest,
          activeIdByModule: {
            ...s.activeIdByModule,
            [module]: sessionId,
          },
          streamingConversationIds,
          abortControllers,
        };
      });
      convId = sessionId;
    };

    let acc = "";
    let thoughtsAcc = "";
    try {
      await streamChat(
        {
          module,
          mode,
          prompt: text,
          sessionId: conversation.sessionId,
          attachments: opts?.attachments,
          redesignOption: opts?.redesignOption,
          pubmedYear: opts?.pubmedYear,
          pubmedArticleType: opts?.pubmedArticleType,
        },
        (e) => {
          if (controller.signal.aborted) return;
          if (e.sessionId) {
            migrateToSessionId(e.sessionId);
          }
          if (e.thoughts) {
            thoughtsAcc = appendStreamDelta(thoughtsAcc, e.thoughts);
            patchAssistant({ thoughts: thoughtsAcc, status: "streaming" });
          }
          if (e.text) {
            acc = appendStreamDelta(acc, e.text);
            patchAssistant({ content: acc, status: "streaming" });
          }
          if (
            mode === "web" &&
            typeof e.webPageCount === "number" &&
            e.webPageCount > 0
          ) {
            patchAssistant({
              webPageCount: e.webPageCount,
              retrievalSummary: chatCopy().readWebPages(e.webPageCount),
            });
          }
          if (e.docReferences) patchAssistant({ docReferences: e.docReferences });
          if (e.table) patchAssistant({ table: e.table });
          if (e.finishReason) {
            patchAssistant({
              status: e.finishReason === "stopped" ? "stopped" : "done",
              finishReason: e.finishReason,
            });
          }
        },
        controller.signal,
      );
      const msgs = get().messagesByConversation[convId] ?? [];
      const last = msgs.find((m) => m.id === assistantMsg.id);
      if (
        !controller.signal.aborted &&
        last &&
        (last.status === "pending" || last.status === "streaming")
      ) {
        patchAssistant({ status: "done" });
      }
      if (mode === "web") {
        const current = get().messagesByConversation[convId]?.find((m) => m.id === assistantMsg.id);
        const n = current?.webPageCount ?? countWebPages({ content: current?.content ?? acc });
        if (n > 0) {
          patchAssistant({
            webPageCount: n,
            retrievalSummary: chatCopy().readWebPages(n),
          });
        } else if (isRetrievingWeb(current?.retrievalSummary)) {
          patchAssistant({ retrievalSummary: undefined });
        }
      }
    } catch (err) {
      const aborted = (err as Error)?.name === "AbortError";
      const current = get().messagesByConversation[convId]?.find((m) => m.id === assistantMsg.id);
      patchAssistant({
        status: aborted ? "stopped" : "error",
        content: aborted
          ? acc || current?.content || ""
          : acc || ((err as Error)?.message ?? chatCopy().chatFailed),
      });
    } finally {
      set((s) => {
        if (s.abortControllers[convId] !== controller) return {};
        const streamingConversationIds = { ...s.streamingConversationIds };
        const abortControllers = { ...s.abortControllers };
        delete streamingConversationIds[convId];
        delete abortControllers[convId];
        return { streamingConversationIds, abortControllers };
      });
    }
  },

  stopGenerating: (conversationId) => {
    if (!conversationId) return;
    const { abortControllers, messagesByConversation } = get();
    abortControllers[conversationId]?.abort();
    const msgs = messagesByConversation[conversationId] ?? [];
    const lastAssistant = [...msgs].reverse().find((m) => m.role === "assistant");
    set((s) => {
      const streamingConversationIds = { ...s.streamingConversationIds };
      const nextAbortControllers = { ...s.abortControllers };
      delete streamingConversationIds[conversationId];
      delete nextAbortControllers[conversationId];
      return {
        messagesByConversation: lastAssistant
          ? {
            ...s.messagesByConversation,
            [conversationId]: msgs.map((m) =>
              m.id === lastAssistant.id &&
              (m.status === "pending" || m.status === "streaming")
                ? { ...m, status: "stopped" as const }
                : m,
            ),
          }
          : messagesByConversation,
        streamingConversationIds,
        abortControllers: nextAbortControllers,
      };
    });
  },
}));
