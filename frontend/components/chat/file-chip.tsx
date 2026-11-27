"use client";

import { FileText, Loader2, X } from "lucide-react";
import { AttachmentPreview } from "@/components/chat/attachment-preview";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import { cn } from "@/lib/utils";
import type { Attachment } from "@/types/chat";

function formatFileSize(bytes: number) {
  if (!bytes) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(bytes < 10 * 1024 ? 1 : 0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function fileExtension(name: string) {
  return name.split(".").pop()?.toUpperCase() || "FILE";
}

export function FileChip({ attachment, onRemove }: { attachment: Attachment; onRemove?: () => void }) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const canPreview = attachment.status === "done" && Boolean(attachment.url || attachment.sourceFile);

  return (
    <AttachmentPreview attachment={attachment} disabled={!canPreview}>
      <div
        role={canPreview ? "button" : undefined}
        tabIndex={canPreview ? 0 : undefined}
        aria-label={canPreview ? `${copy.previewAttachment}: ${attachment.name}` : undefined}
        className={cn(
          "group relative flex min-w-0 max-w-72 items-center gap-3 rounded-xl border bg-card px-3 py-2.5 text-left shadow-sm",
          canPreview && "cursor-pointer transition-colors hover:border-brand/40 hover:bg-muted/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30",
          attachment.status === "error" && "border-destructive/40",
        )}
      >
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-500 text-white">
          {attachment.status === "uploading" ? <Loader2 className="size-5 animate-spin" /> : <FileText className="size-5" />}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-foreground">{attachment.name}</span>
          <span className="mt-0.5 block text-xs text-muted-foreground">
            {fileExtension(attachment.name)}{formatFileSize(attachment.size) ? ` ${formatFileSize(attachment.size)}` : ""}
          </span>
        </span>
        {onRemove && (
          <button
            type="button"
            onClick={(event) => { event.stopPropagation(); onRemove(); }}
            aria-label={copy.removeAttachment}
            className="shrink-0 rounded-full p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <X className="size-3.5" />
          </button>
        )}
      </div>
    </AttachmentPreview>
  );
}
