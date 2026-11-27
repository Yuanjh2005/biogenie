import { env } from "@/lib/config/env";
import { getAgent } from "@/lib/config/agents";
import {
  designModePromptHint,
  pubmedTypeConstraint,
  pubmedYearConstraint,
  type PubmedArticleType,
  type PubmedYear,
} from "@/lib/config/chat-filters";
import { getRefreshToken, getToken } from "@/lib/auth/token";
import {
  captureRenewedAccessToken,
  refreshAccessToken,
} from "@/lib/auth/refresh";
import { parseSSE } from "@/lib/sse/parse";
import { countWebPages, type ThoughtLike } from "@/lib/chat/web-pages";
import { appendStreamDelta } from "@/lib/chat/stream-text";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import { ApiError, triggerUnauthorized } from "./errors";
import { mockChatStream } from "./mock";
import type { ChatCompletionParams, ChatStreamEvent } from "@/types/chat";

interface BackendDocReference {
  indexId?: string;
  index_id?: string;
  title?: string;
  docName?: string;
  doc_name?: string;
  docUrl?: string;
  doc_url?: string;
  url?: string;
}

function chatCopy() {
  return CHAT_COPY[useLocaleStore.getState().locale];
}

interface BackendStreamEvent {
  type?: string;
  text?: string;
  thoughts?: ThoughtLike[];
  docReferences?: BackendDocReference[];
  doc_references?: BackendDocReference[];
  sessionId?: string;
  finishReason?: string;
  message?: string;
}

function thoughtsToText(thoughts?: ThoughtLike[]): string | undefined {
  if (!thoughts?.length) return undefined;
  const parts = thoughts
    .map((t) => t.thought || "")
    .filter(Boolean);
  return parts.length ? parts.join("") : undefined;
}

function buildPrompt(params: ChatCompletionParams): string {
  const extras: string[] = [];
  const designHint = designModePromptHint(params.redesignOption ?? "");
  if (designHint) {
    extras.push(`【设计方式】${designHint}`);
  }
  const yearHint = pubmedYearConstraint((params.pubmedYear ?? "unlimited") as PubmedYear);
  if (yearHint) extras.push(`【发表年限】${yearHint}`);
  const typeHint = pubmedTypeConstraint(
    (params.pubmedArticleType ?? "unlimited") as PubmedArticleType,
  );
  if (typeHint) extras.push(`【文献类型】${typeHint}。请按所选类型检索并在结果中体现。`);
  if (extras.length === 0) return params.prompt;
  return `${params.prompt}\n\n${extras.join("\n")}`;
}

function normalizeDocReferences(refs?: BackendDocReference[]) {
  if (!refs?.length) return undefined;
  const normalized = refs
    .map((ref, i) => {
      const rawIndex = ref.indexId ?? ref.index_id ?? "";
      const parsedIndex = Number(rawIndex.match(/\d+/)?.[0]);
      const url = ref.docUrl ?? ref.doc_url ?? ref.url;
      // doc_name 在 PubMed 返回中通常是“作者, 标题, 期刊, 年份, PMID”完整引文，
      // title 则往往只有论文标题；优先保留信息更完整的字段供引用卡片展示。
      const citation = [ref.title, ref.docName, ref.doc_name]
        .filter((value): value is string => Boolean(value?.trim()))
        .sort((a, b) => b.length - a.length)[0] ?? url;
      const title = ref.title ?? ref.docName ?? ref.doc_name ?? url;
      if (!title || !citation) return null;
      return {
        index: Number.isFinite(parsedIndex) && parsedIndex > 0 ? parsedIndex : i + 1,
        title,
        citation,
        url,
      };
    })
    .filter((ref): ref is NonNullable<typeof ref> => ref !== null);
  return normalized.length ? normalized : undefined;
}

async function fetchChatStream(
  requestBody: object,
  signal?: AbortSignal,
  canRetryAuth = true,
): Promise<Response> {
  const token = getToken();
  const response = await fetch(`${env.apiBaseUrl.replace(/\/$/, "")}/ai/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(requestBody),
    signal,
  });

  captureRenewedAccessToken(response);

  let authCode = NaN;
  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("text/event-stream")) {
    try {
      const payload = (await response.clone().json()) as { code?: number | string };
      authCode = payload.code != null ? Number(payload.code) : NaN;
    } catch {
      // 非 JSON 错误响应由调用方处理。
    }
  }

  const accessRejected =
    response.status === 401 || authCode === 40100 || authCode === 40101;
  if (accessRejected && canRetryAuth && getRefreshToken()) {
    try {
      await refreshAccessToken();
      return fetchChatStream(requestBody, signal, false);
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error;
    }
  }

  if (accessRejected || authCode === 40102) {
    triggerUnauthorized();
    throw new ApiError(
      chatCopy().sessionExpired,
      401,
      Number.isNaN(authCode) ? undefined : authCode,
    );
  }

  return response;
}

/**
 * 流式对话。回调 onEvent 接收增量事件。
 * - env.useMockChat=true：走 mock（仅当需要本地演示时）。
 * - 否则：POST /ai/chat/stream（SSE: delta / done / error）。
 */
export async function streamChat(
  params: ChatCompletionParams,
  onEvent: (e: ChatStreamEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  if (env.useMockChat) {
    for await (const e of mockChatStream(params, signal)) {
      onEvent(e);
    }
    return;
  }

  const agent = getAgent(params.module, params.mode);
  if (!agent) {
    throw new ApiError(chatCopy().modeUnavailable, 400);
  }

  const attachments = params.attachments
    ?.filter((a) => a.status === "done" && a.url)
    .map((a) => ({
      url: a.url as string,
      name: a.name,
      contentType: a.type || undefined,
    }));

  // 设计方式 / 文献筛选暂无独立后端字段，拼进 prompt 传递
  const prompt = buildPrompt(params);

  const requestBody = {
    prompt,
    sessionId: params.sessionId,
    app: agent.app,
    enableThinking: agent.enableThinking ?? false,
    hasThoughts: agent.hasThoughts ?? false,
    ...(attachments?.length ? { attachments } : {}),
  };

  const res = await fetchChatStream(requestBody, signal);

  const contentType = res.headers.get("content-type") || "";
  if (!res.ok || !res.body) {
    throw new ApiError(chatCopy().chatServiceError, res.status);
  }

  // 鉴权失败时后端可能仍返回 JSON（非 SSE）
  if (!contentType.includes("text/event-stream")) {
    try {
      const payload = (await res.json()) as {
        code?: number | string;
        message?: string;
      };
      const code = payload.code != null ? Number(payload.code) : NaN;
      if (code === 40100 || code === 40101 || code === 40102) {
        triggerUnauthorized();
        throw new ApiError(payload.message || chatCopy().sessionExpired, 401, code);
      }
      throw new ApiError(payload.message || chatCopy().chatServiceError, res.status, payload.code);
    } catch (e) {
      if (e instanceof ApiError) throw e;
      throw new ApiError(chatCopy().chatServiceError, res.status);
    }
  }

  let streamError: ApiError | null = null;
  let receivedDeltaText = false;
  const thoughtAcc: ThoughtLike[] = [];
  let accText = "";
  await parseSSE(
    res.body,
    (data, eventName) => {
      if (streamError) return;
      if (data === "[DONE]") {
        onEvent({ finishReason: "stop" });
        return;
      }
      try {
        const json = JSON.parse(data) as BackendStreamEvent;
        const type = eventName || json.type;

        if (type === "error") {
          streamError = new ApiError(json.message || chatCopy().chatFailed, 500);
          return;
        }

        if (json.thoughts?.length) thoughtAcc.push(...json.thoughts);
        const thoughts = thoughtsToText(json.thoughts);
        if (type === "delta" && json.text) {
          receivedDeltaText = true;
          accText = appendStreamDelta(accText, json.text);
        }

        // done.text 在已有 delta 时多为全文，避免重复；无 delta 时回退使用
        const text =
          type === "done" && receivedDeltaText ? undefined : json.text;
        if (type === "done" && !receivedDeltaText && json.text) {
          accText = json.text;
        }

        const webPageCount =
          params.mode === "web"
            ? countWebPages({
                thoughts: thoughtAcc,
                content: accText,
              })
            : 0;

        onEvent({
          text,
          thoughts: type === "done" ? undefined : thoughts,
          docReferences: normalizeDocReferences(
            json.docReferences ?? json.doc_references,
          ),
          sessionId: json.sessionId,
          webPageCount: webPageCount > 0 ? webPageCount : undefined,
          finishReason:
            type === "done"
              ? json.finishReason || "stop"
              : json.finishReason,
        });
      } catch {
        // 忽略无法解析的心跳/注释
      }
    },
    signal,
  );
  if (streamError) throw streamError;
}
