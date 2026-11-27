"use client";

import { useEffect, useLayoutEffect, useRef } from "react";
import { MessageBubble } from "@/components/chat/message-bubble";
import type { PaperAction } from "@/components/chat/doc-references";
import { getScrollParent, scrollChildIntoContainer } from "@/lib/chat/scroll-container";
import { CHAT_COLUMN_CLASS } from "@/lib/config/layout";
import { cn } from "@/lib/utils";
import type { Attachment, ChatMessage, DocReference } from "@/types/chat";

interface MessageListProps {
  messages: ChatMessage[];
  streamingId: string | null;
  onRegenerate?: (assistantIndex: number) => void;
  onEdit?: (message: ChatMessage, content: string, attachments: Attachment[]) => void;
  /** 当前会话 sessionId，用于整段分享 */
  sessionId?: string;
  onPaperAction?: (action: PaperAction, paper: DocReference) => void;
}

const BOTTOM_THRESHOLD_PX = 80;

function isNearBottom(scroller: HTMLElement): boolean {
  return scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight < BOTTOM_THRESHOLD_PX;
}

export function MessageList({
  messages,
  streamingId,
  onRegenerate,
  onEdit,
  sessionId,
  onPaperAction,
}: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const stickToBottom = useRef(true);
  const lastMessageId = useRef<string | undefined>(undefined);
  const prevStreamingId = useRef<string | null>(streamingId);
  const lastId = messages[messages.length - 1]?.id;

  useEffect(() => {
    const scroller = getScrollParent(bottomRef.current);
    if (!scroller) return;
    let leftBottom = false;

    const onWheel = (e: WheelEvent) => {
      if (e.deltaY < 0) {
        // 鼠标向上滚：立刻松开自动吸底，方便查看上方内容
        stickToBottom.current = false;
      } else if (isNearBottom(scroller)) {
        stickToBottom.current = true;
        leftBottom = false;
      }
    };
    const onScroll = () => {
      if (!isNearBottom(scroller)) {
        leftBottom = true;
        stickToBottom.current = false;
        return;
      }
      // 只有先离开底部再滚回来，才恢复吸底，避免触控板微移被立刻拽回去
      if (leftBottom) {
        stickToBottom.current = true;
        leftBottom = false;
      }
    };

    scroller.addEventListener("wheel", onWheel, { passive: true });
    scroller.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      scroller.removeEventListener("wheel", onWheel);
      scroller.removeEventListener("scroll", onScroll);
    };
  }, []);

  useLayoutEffect(() => {
    const stoppedStreaming = Boolean(prevStreamingId.current) && !streamingId;
    prevStreamingId.current = streamingId;

    if (lastId !== lastMessageId.current) {
      stickToBottom.current = true;
      lastMessageId.current = lastId;
    }

    // 点击停止后松开吸底，并把已生成内容滚进消息列表，避免停在底部空白处
    if (stoppedStreaming) {
      const wasSticking = stickToBottom.current;
      stickToBottom.current = false;
      if (wasSticking) {
        const lastBubble = bottomRef.current?.previousElementSibling;
        if (lastBubble instanceof HTMLElement) {
          scrollChildIntoContainer(lastBubble);
        }
      }
      window.scrollTo(0, 0);
      return;
    }

    if (!stickToBottom.current) return;
    const scroller = getScrollParent(bottomRef.current);
    if (scroller) scroller.scrollTop = scroller.scrollHeight;
  }, [messages, lastId, streamingId]);

  return (
    <div className={cn(CHAT_COLUMN_CLASS, "[overflow-anchor:none] space-y-5 py-6")}>
      {messages.map((m, i) => {
        // 助手消息对应的问题是它前面的用户消息
        const question =
          m.role === "assistant" && i > 0 && messages[i - 1].role === "user"
            ? messages[i - 1].content
            : "";
        return (
          <MessageBubble
            key={m.id}
            message={m}
            question={question}
            streaming={streamingId !== null && m.status === "streaming"}
            onRegenerate={m.role === "assistant" ? () => onRegenerate?.(i) : undefined}
            onEdit={m.role === "user" ? (content, attachments) => onEdit?.(m, content, attachments) : undefined}
            editDisabled={streamingId !== null}
            sessionId={sessionId}
            onPaperAction={m.role === "assistant" ? onPaperAction : undefined}
          />
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
