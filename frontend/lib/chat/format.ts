import type { ChatMessage, DocReference } from "@/types/chat";
import { CHAT_COPY } from "@/lib/i18n/chat";
import type { Locale } from "@/stores/locale";
import { formatPaperCitation } from "@/lib/chat/papers";

function formatReferences(refs: DocReference[] | undefined, locale: Locale): string {
  if (!refs || refs.length === 0) return "";
  const lines = refs.map((r, i) => {
    const idx = r.index ?? i + 1;
    return `[${idx}] ${formatPaperCitation(r)}`;
  });
  return `\n\n${CHAT_COPY[locale].copyReferences}\n${lines.join("\n")}`;
}

/**
 * 生成复制/分享文本：问题 + 回答 + 引用（无引用则省略）。
 * 严格不包含 Key / token / request_id 等敏感信息。
 */
export function buildCopyText(
  question: string,
  answer: ChatMessage,
  locale: Locale = "zh-CN",
): string {
  const q = question.trim();
  const copy = CHAT_COPY[locale];
  const parts: string[] = [];
  if (q) parts.push(copy.copyQuestion(q));
  parts.push(copy.copyAnswer(answer.content.trim()));
  const refs = formatReferences(answer.docReferences, locale);
  return parts.join("\n\n") + refs;
}
