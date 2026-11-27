import { scrollChildIntoContainer } from "@/lib/chat/scroll-container";

/** 将正文中的 [7] 转为可点击锚点，并定位到同页参考文献。 */

const PROTECTED_CONTENT_RE = /(```[\s\S]*?```|`[^`\n]+`|<a\b[^>]*>[\s\S]*?<\/a>)/gi;
const UNSAFE_ID_RE = /[^a-zA-Z0-9_-]/g;
const CITE_CLUSTER_RE = /\[(\d+(?:\s*[,，、]\s*\d+)+)\](?!\s*[:(])/g;
const CITE_DOUBLE_RE = /\[\[(\d+)\]\]/g;
const CITE_FOOTNOTE_RE = /\[\^(\d+)\]/g;
const CITE_FULLWIDTH_RE = /［(\d+)］|【(\d+)】/g;
const CITE_SINGLE_RE = /\[(\d+)\](?!\s*[:(])/g;

export function citationAnchorId(scope: string, index: number | string): string {
  const safeScope = scope.replace(UNSAFE_ID_RE, "");
  return `cite-${safeScope}-${index}`;
}

function citeLink(
  scope: string,
  index: string,
  citationUrls?: Readonly<Record<string, string>>,
): string {
  const externalUrl = citationUrls?.[index];
  const href = /^https?:\/\//i.test(externalUrl ?? "")
    ? externalUrl
    : `#${citationAnchorId(scope, index)}`;
  return `[\\[${index}\\]](${href})`;
}

function linkifyInText(
  text: string,
  scope: string,
  citationUrls?: Readonly<Record<string, string>>,
): string {
  let out = text.replace(CITE_DOUBLE_RE, (_m, n: string) =>
    citeLink(scope, n, citationUrls),
  );
  out = out.replace(CITE_FOOTNOTE_RE, (_m, n: string) =>
    citeLink(scope, n, citationUrls),
  );
  out = out.replace(CITE_FULLWIDTH_RE, (_m, a: string, b: string) =>
    citeLink(scope, a || b, citationUrls),
  );
  out = out.replace(CITE_CLUSTER_RE, (_m, cluster: string) =>
    cluster
      .split(/[,，、]/)
      .map((part) => part.trim())
      .filter(Boolean)
      .map((n) => citeLink(scope, n, citationUrls))
      .join(", "),
  );
  return out.replace(CITE_SINGLE_RE, (_m, n: string) =>
    citeLink(scope, n, citationUrls),
  );
}

/**
 * 把正文里的 [7] / [[7]] / [^7] / [1,2,7] 转成 Markdown 锚点。
 * `[^7]: 来源` 形式也会转换，使脚注编号可显示并成为正文引用的跳转目标。
 * 跳过代码块和已有 HTML 链接，兼容 DesignMCP 返回的 <a id="ref1"> 锚点。
 */
export function linkifyCitationMarkers(
  markdown: string,
  scope: string,
  citationUrls?: Readonly<Record<string, string>>,
): string {
  if (!markdown || !scope) return markdown;
  return markdown
    .split(PROTECTED_CONTENT_RE)
    .map((part) =>
      part.startsWith("`") || /^<a\b/i.test(part)
        ? part
        : linkifyInText(part, scope, citationUrls),
    )
    .join("");
}

export function citationIndexFromHref(href: string, scope: string): string | undefined {
  const raw = href.startsWith("#") ? href.slice(1) : href;
  const prefix = `${citationAnchorId(scope, "")}`;
  if (raw.startsWith(prefix)) {
    const index = raw.slice(prefix.length);
    return /^\d+$/.test(index) ? index : undefined;
  }
  const m = raw.match(/^(?:ref-?|fn-?|#)?(\d+)$/i);
  return m?.[1];
}

function citationCandidateIds(scope: string, index: string, href?: string): string[] {
  return [
    href?.startsWith("#") ? decodeURIComponent(href.slice(1)) : undefined,
    citationAnchorId(scope, index),
    `ref-${index}`,
    `ref${index}`,
    `fn-${index}`,
    `fn${index}`,
  ].filter((id): id is string => Boolean(id));
}

function queryById(root: ParentNode | Document, id: string): HTMLElement | null {
  try {
    const el = root.querySelector(`#${CSS.escape(id)}`);
    return el instanceof HTMLElement ? el : null;
  } catch {
    return null;
  }
}

export function findCitationElement(
  scope: string,
  index: string,
  href?: string,
): HTMLElement | null {
  for (const id of citationCandidateIds(scope, index, href)) {
    const el = document.getElementById(id);
    if (el) return el;
  }
  return null;
}

/** 在当前消息根节点内查找锚点，避免多条消息共用 id="ref1" 时跳错。 */
export function findCitationInRoot(
  root: ParentNode,
  href: string,
  scope?: string,
): HTMLElement | null {
  const raw = href.startsWith("#") ? href.slice(1) : href;
  let id = raw;
  try {
    id = decodeURIComponent(raw);
  } catch {
    id = raw;
  }
  const local = queryById(root, id);
  if (local) return local;

  const index = citationIndexFromHref(href, scope ?? "");
  if (!index) return null;
  for (const candidate of citationCandidateIds(scope ?? "", index, href)) {
    const el = queryById(root, candidate);
    if (el) return el;
  }
  return findCitationElement(scope ?? "", index, href);
}

/** 空的 <a id="ref1"></a> 没有高度，闪一下/滚动时用它的块级父节点。 */
function citationFlashTarget(el: HTMLElement): HTMLElement {
  if (el.tagName === "A" && !el.textContent?.trim()) {
    const block = el.closest("p, li, h1, h2, h3, h4, strong");
    if (block instanceof HTMLElement) return block;
  }
  return el;
}

export function scrollToCitation(el: HTMLElement): void {
  const target = citationFlashTarget(el);
  scrollChildIntoContainer(target, { offset: 16 });
  target.classList.remove("cite-flash");
  // 强制重绘后再加 class，重复点击同一条也能闪一下
  void target.offsetWidth;
  target.classList.add("cite-flash");
  window.setTimeout(() => target.classList.remove("cite-flash"), 1200);
}
