import { CHAT_COPY } from "@/lib/i18n/chat";
import type { Locale } from "@/stores/locale";

export interface ThoughtLike {
  thought?: string;
  response?: string;
  actionType?: string;
  action_type?: string;
  actionName?: string;
  action_name?: string;
  action?: string;
}

const URL_RE = /https?:\/\/[^\s"'<>)\]}]+/g;
const MD_LINK_RE = /\[[^\]]*]\((https?:\/\/[^)\s]+)\)/g;
const REPORTED_PAGE_COUNT_RES = [
  /\bread\s+(\d+)\s+web\s*pages?\b/gi,
  /(?:已?阅读|读取|访问|浏览|检索)(?:了|过|共|到)?\s*(\d+)\s*个?\s*(?:网页|页面|网站)/g,
  /(?:检索|搜索)\s*共?\s*(?:访问|阅读|获取|找到)?\s*(\d+)\s*个?\s*(?:网页|页面|结果)/g,
];

function isSearchThought(item: ThoughtLike): boolean {
  const blob = [item.actionType, item.action_type, item.actionName, item.action_name, item.action]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
  return /search|web|quark|bing|google|检索|联网/.test(blob);
}

function urlsFromText(text: string | undefined): string[] {
  if (!text) return [];
  const urls = new Set<string>();
  for (const match of text.matchAll(MD_LINK_RE)) {
    urls.add(normalizeUrl(match[1]));
  }
  for (const match of text.matchAll(URL_RE)) {
    urls.add(normalizeUrl(match[0]));
  }
  return [...urls];
}

function normalizeUrl(raw: string): string {
  return raw.replace(/[.,;]+$/, "").replace(/\/+$/, "").toLowerCase();
}

function reportedPageCount(text: string | undefined): number {
  if (!text) return 0;
  let count = 0;
  for (const pattern of REPORTED_PAGE_COUNT_RES) {
    pattern.lastIndex = 0;
    for (const match of text.matchAll(pattern)) {
      const value = Number(match[1]);
      if (Number.isFinite(value)) count = Math.max(count, value);
    }
  }
  return count;
}

function urlsFromJson(text: string | undefined): string[] {
  if (!text) return [];
  const trimmed = text.trim();
  if (!(trimmed.startsWith("[") || trimmed.startsWith("{"))) return [];
  try {
    const parsed: unknown = JSON.parse(trimmed);
    const items = Array.isArray(parsed)
      ? parsed
      : parsed && typeof parsed === "object" && "results" in parsed
        ? (parsed as { results?: unknown }).results
        : parsed && typeof parsed === "object" && "data" in parsed
          ? (parsed as { data?: unknown }).data
          : [parsed];
    if (!Array.isArray(items)) return [];
    const urls: string[] = [];
    for (const item of items) {
      if (!item || typeof item !== "object") continue;
      const rec = item as Record<string, unknown>;
      const url = rec.url ?? rec.link ?? rec.href ?? rec.sourceUrl;
      if (typeof url === "string" && url.startsWith("http")) {
        urls.push(normalizeUrl(url));
      }
    }
    return urls;
  } catch {
    return [];
  }
}

/** 从思考过程与成文内容中统计去重后的网页数 */
export function countWebPages(params: {
  thoughts?: ThoughtLike[];
  content?: string;
}): number {
  const urls = new Set<string>();
  let reportedCount = reportedPageCount(params.content);
  for (const item of params.thoughts ?? []) {
    reportedCount = Math.max(
      reportedCount,
      reportedPageCount(item.thought),
      reportedPageCount(item.response),
    );
    for (const url of urlsFromJson(item.response)) urls.add(url);
    for (const url of urlsFromText(item.response)) urls.add(url);
    if (isSearchThought(item)) {
      for (const url of urlsFromText(item.thought)) urls.add(url);
      for (const url of urlsFromJson(item.thought)) urls.add(url);
    }
  }
  for (const url of urlsFromText(params.content)) urls.add(url);
  return Math.max(urls.size, reportedCount);
}

export function webPagesSummary(
  count: number,
  streaming: boolean,
  locale: Locale = "zh-CN",
): string | undefined {
  const copy = CHAT_COPY[locale];
  if (count > 0) return copy.readWebPages(count);
  if (streaming) return copy.retrievingWeb;
  return undefined;
}
