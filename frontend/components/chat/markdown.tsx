"use client";

import {
  Children,
  isValidElement,
  useMemo,
  type MouseEvent,
  type ReactNode,
} from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import "highlight.js/styles/github.css";
import { cn } from "@/lib/utils";
import { CopyableHtmlTable } from "@/components/chat/copy-table-button";
import {
  citationAnchorId,
  citationIndexFromHref,
  findCitationElement,
  findCitationInRoot,
  linkifyCitationMarkers,
  scrollToCitation,
} from "@/lib/chat/citations";
import {
  hasChatHtml,
  renderChatHtml,
} from "@/lib/chat/sanitize-html";

function getNodeText(node: ReactNode): string {
  if (typeof node === "string" || typeof node === "number") return String(node);
  if (Array.isArray(node)) return node.map(getNodeText).join("");
  if (isValidElement<{ children?: ReactNode }>(node)) {
    return getNodeText(node.props.children);
  }
  return "";
}

/** 找到渲染内容中的第一个有效节点；参考文献编号可能被 strong 等标签包裹。 */
function leadingLinkHref(node: ReactNode): { href?: string; found: boolean } {
  if (typeof node === "string" || typeof node === "number") {
    return { found: String(node).trim().length > 0 };
  }
  if (Array.isArray(node)) {
    for (const child of node) {
      const result = leadingLinkHref(child);
      if (result.found) return result;
    }
    return { found: false };
  }
  if (isValidElement<{ href?: string; children?: ReactNode }>(node)) {
    if (node.props.href) return { href: node.props.href, found: true };
    return leadingLinkHref(node.props.children);
  }
  return { found: false };
}

/** 文末「[7] 标题…」条目：给段落加上锚点，供正文引用跳转。 */
function bibliographyIndex(children: ReactNode, scope?: string): string | undefined {
  if (!scope) return undefined;
  const nodes = Children.toArray(children);
  const href = leadingLinkHref(nodes).href;
  if (!href?.startsWith("#")) return undefined;
  const prefix = `#${citationAnchorId(scope, "")}`;
  if (!href.startsWith(prefix)) return undefined;
  const index = href.slice(prefix.length);
  if (!/^\d+$/.test(index)) return undefined;
  const rest = getNodeText(nodes.slice(1)).trim();
  if (rest.length < 8) return undefined;
  return index;
}

function omitMdNode<T extends object>(props: T): T {
  delete (props as { node?: unknown }).node;
  return props;
}

function jumpHashLink(
  event: MouseEvent<HTMLElement>,
  href: string | undefined,
  root: ParentNode,
  citeScope?: string,
  linkText?: string,
): void {
  if (!href?.startsWith("#")) return;
  event.preventDefault();
  const text = linkText?.trim() ?? "";
  const index =
    (/^\d+$/.test(text) ? text : undefined) ??
    citationIndexFromHref(href, citeScope ?? "");
  const el = index
    ? (findCitationInRoot(root, href, citeScope) ??
      findCitationElement(citeScope ?? "", index, href))
    : findCitationInRoot(root, href, citeScope);
  if (el) scrollToCitation(el);
}

function ChatHtml({
  content,
  className,
  citeScope,
  citationUrls,
}: {
  content: string;
  className?: string;
  citeScope?: string;
  citationUrls?: Readonly<Record<string, string>>;
}) {
  const html = useMemo(() => {
    const source = citeScope
      ? linkifyCitationMarkers(content, citeScope, citationUrls)
      : content;
    return renderChatHtml(source);
  }, [content, citeScope, citationUrls]);

  const onClick = (event: MouseEvent<HTMLDivElement>) => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const anchor = target.closest("a");
    if (!anchor || !event.currentTarget.contains(anchor)) return;
    const href = anchor.getAttribute("href") ?? undefined;
    if (!href?.startsWith("#")) return;
    jumpHashLink(event, href, event.currentTarget, citeScope, anchor.textContent ?? "");
  };

  return (
    <div
      className={cn("chat-markdown", className)}
      onClick={onClick}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

export function Markdown({
  content,
  className,
  citeScope,
  citationUrls,
}: {
  content: string;
  className?: string;
  citeScope?: string;
  citationUrls?: Readonly<Record<string, string>>;
}) {
  if (hasChatHtml(content)) {
    return (
      <ChatHtml
        content={content}
        className={className}
        citeScope={citeScope}
        citationUrls={citationUrls}
      />
    );
  }

  const md = citeScope
    ? linkifyCitationMarkers(content, citeScope, citationUrls)
    : content;

  const jumpToCite = (
    event: MouseEvent<HTMLAnchorElement>,
    href: string | undefined,
    children: ReactNode,
  ) => {
    if (!citeScope) return;
    jumpHashLink(event, href, event.currentTarget.ownerDocument, citeScope, getNodeText(children));
  };

  return (
    <div className={cn("chat-markdown", className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        components={{
          table: ({ children }) => <CopyableHtmlTable>{children}</CopyableHtmlTable>,
          a: ({ href, children, ...props }) => {
            const external = /^https?:\/\//i.test(href ?? "");
            const isHash = Boolean(href?.startsWith("#"));
            return (
              <a
                {...omitMdNode(props)}
                href={href}
                target={external ? "_blank" : undefined}
                rel={external ? "noopener noreferrer" : undefined}
                onClick={(e) => {
                  if (isHash) e.preventDefault();
                  jumpToCite(e, href, children);
                }}
              >
                {children}
              </a>
            );
          },
          p: ({ children, className, ...props }) => {
            const index = bibliographyIndex(children, citeScope);
            const id = index && citeScope ? citationAnchorId(citeScope, index) : undefined;
            return (
              <p {...omitMdNode(props)} id={id} className={cn(id && "scroll-mt-4", className)}>
                {children}
              </p>
            );
          },
          li: ({ children, className, ...props }) => {
            const index = bibliographyIndex(children, citeScope);
            const id = index && citeScope ? citationAnchorId(citeScope, index) : undefined;
            return (
              <li {...omitMdNode(props)} id={id} className={cn(id && "scroll-mt-4", className)}>
                {children}
              </li>
            );
          },
        }}
      >
        {md}
      </ReactMarkdown>
    </div>
  );
}
