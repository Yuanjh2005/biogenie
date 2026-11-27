"use client";

import { useState } from "react";
import { Check, Copy, Loader2, RefreshCw, Share2, ThumbsDown, ThumbsUp } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { copyToClipboard } from "@/lib/clipboard";
import { buildCopyText } from "@/lib/chat/format";
import { conversationApi } from "@/lib/api/conversations";
import { ApiError } from "@/lib/api/errors";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import type { ChatMessage } from "@/types/chat";

interface MessageActionsProps {
  message: ChatMessage;
  question: string;
  onRegenerate?: () => void;
  disabled?: boolean;
  /** 有 sessionId 时创建整段会话分享链接 */
  sessionId?: string;
}

export function MessageActions({
  message,
  question,
  onRegenerate,
  disabled,
  sessionId,
}: MessageActionsProps) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const [copied, setCopied] = useState(false);
  const [sharing, setSharing] = useState(false);
  const [vote, setVote] = useState<"up" | "down" | null>(null);

  const handleCopy = async () => {
    const ok = await copyToClipboard(buildCopyText(question, message, locale));
    if (!ok) {
      toast.error(copy.copyFailed);
      return;
    }
    setCopied(true);
    toast.success(copy.copied);
    setTimeout(() => setCopied(false), 1500);
  };

  const share = async () => {
    if (!sessionId) {
      toast.error(copy.shareNotReady);
      return;
    }
    if (sharing) return;
    setSharing(true);
    try {
      const result = await conversationApi.share(sessionId);
      const url = result.shareUrl || `${window.location.origin}/share/${result.shareToken}`;
      const ok = await copyToClipboard(url);
      if (ok) {
        toast.success(copy.shareCopied);
        return;
      }
      toast.success(copy.shareGenerated, {
        description: url,
        duration: 12000,
        action: {
          label: copy.copy,
          onClick: () => {
            void copyToClipboard(url).then((copied) => {
              if (copied) toast.success(copy.copied);
              else toast.error(copy.copyManually);
            });
          },
        },
      });
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : copy.shareFailed);
    } finally {
      setSharing(false);
    }
  };

  return (
    <div className="mt-1.5 flex items-center gap-0.5 text-muted-foreground">
      <ActionButton label={copy.copy} onClick={handleCopy}>
        {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
      </ActionButton>
      {onRegenerate && (
        <ActionButton label={copy.regenerate} onClick={onRegenerate} disabled={disabled}>
          <RefreshCw className="size-3.5" />
        </ActionButton>
      )}
      <ActionButton
        label={copy.like}
        active={vote === "up"}
        onClick={() => setVote((v) => (v === "up" ? null : "up"))}
      >
        <ThumbsUp className="size-3.5" />
      </ActionButton>
      <ActionButton
        label={copy.dislike}
        active={vote === "down"}
        onClick={() => setVote((v) => (v === "down" ? null : "down"))}
      >
        <ThumbsDown className="size-3.5" />
      </ActionButton>
      <ActionButton label={copy.share} onClick={share} disabled={sharing || !sessionId}>
        {sharing ? (
          <Loader2 className="size-3.5 animate-spin" />
        ) : (
          <Share2 className="size-3.5" />
        )}
      </ActionButton>
    </div>
  );
}

function ActionButton({
  label,
  onClick,
  active,
  disabled,
  children,
}: {
  label: string;
  onClick: () => void;
  active?: boolean;
  disabled?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Button
      size="icon"
      variant="ghost"
      className={cn("size-7", active && "text-brand")}
      aria-label={label}
      title={label}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </Button>
  );
}
