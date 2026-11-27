"use client";

import { useState } from "react";
import { Check, Copy, Pencil } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { copyToClipboard } from "@/lib/clipboard";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import type { ChatMessage } from "@/types/chat";

interface UserMessageActionsProps {
  message: ChatMessage;
  onEdit?: () => void;
  editDisabled?: boolean;
}

export function UserMessageActions({
  message,
  onEdit,
  editDisabled,
}: UserMessageActionsProps) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    const ok = await copyToClipboard(message.content);
    if (!ok) {
      toast.error(copy.copyFailed);
      return;
    }
    setCopied(true);
    toast.success(copy.copied);
    window.setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="mt-1 flex justify-end gap-0.5 text-muted-foreground">
      <Button
        size="icon"
        variant="ghost"
        className="size-7"
        aria-label={copy.copy}
        title={copy.copy}
        onClick={handleCopy}
      >
        {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
      </Button>
      {onEdit && (
        <Button
          size="icon"
          variant="ghost"
          className="size-7"
          aria-label={copy.edit}
          title={copy.edit}
          onClick={onEdit}
          disabled={editDisabled}
        >
          <Pencil className="size-3.5" />
        </Button>
      )}
    </div>
  );
}
