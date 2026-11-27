import DOMPurify from "isomorphic-dompurify";
import { marked } from "marked";

/** 与后端约定一致，并补上 Markdown 转 HTML 后常见的排版标签。 */
const ALLOWED_TAGS = [
  "a",
  "strong",
  "em",
  "b",
  "i",
  "p",
  "br",
  "h1",
  "h2",
  "h3",
  "h4",
  "h5",
  "h6",
  "ul",
  "ol",
  "li",
  "blockquote",
  "code",
  "pre",
  "hr",
  "table",
  "thead",
  "tbody",
  "tr",
  "th",
  "td",
];

const ALLOWED_ATTR = ["href", "id", "target", "rel", "class"];

const HTML_TAG_RE = /<(?:a|strong|em|b|i|p|br|h[1-6]|ul|ol|li)\b/i;
let hooksReady = false;

function ensureSanitizeHooks(): void {
  if (hooksReady) return;
  hooksReady = true;
  DOMPurify.removeAllHooks();
  DOMPurify.addHook("afterSanitizeAttributes", (node) => {
    if (node.nodeName !== "A") return;
    const href = node.getAttribute("href") ?? "";
    if (href.startsWith("#")) {
      node.removeAttribute("target");
      node.removeAttribute("rel");
      return;
    }
    if (/^https?:\/\//i.test(href)) {
      node.setAttribute("target", "_blank");
      node.setAttribute("rel", "noopener noreferrer");
    }
  });
}

export function hasChatHtml(content: string): boolean {
  return HTML_TAG_RE.test(content);
}

export function sanitizeChatHtml(dirty: string): string {
  ensureSanitizeHooks();
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
  });
}

/** Markdown（含内嵌 HTML 锚点）→ 消毒后的 HTML。 */
export function renderChatHtml(markdown: string): string {
  const raw = marked.parse(markdown, { async: false, gfm: true });
  return sanitizeChatHtml(raw);
}
