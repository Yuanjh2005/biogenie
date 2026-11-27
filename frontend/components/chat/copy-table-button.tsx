"use client";

import { useRef, type ComponentProps } from "react";
import { Copy } from "lucide-react";
import { toast } from "sonner";
import { copyText, htmlTableToTsv, tableToTsv } from "@/lib/chat/copy-table";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";

export function CopyTableButton({ onCopy }: { onCopy: () => string | Promise<string> }) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const handleCopy = async () => {
    const text = await onCopy();
    if (!text.trim()) {
      toast.error(copy.tableEmpty);
      return;
    }
    const ok = await copyText(text);
    if (ok) toast.success(copy.tableCopied);
    else toast.error(copy.copyFailed);
  };

  return (
    <button
      type="button"
      onClick={() => void handleCopy()}
      className="inline-flex items-center gap-1 rounded-md border bg-background/90 px-2 py-1 text-xs text-muted-foreground hover:bg-muted hover:text-foreground"
      aria-label={copy.copyTable}
      title={copy.copyTable}
    >
      <Copy className="size-3.5" />
      {copy.copy}
    </button>
  );
}

export function CopyableHtmlTable({
  children,
  ...props
}: ComponentProps<"table">) {
  const ref = useRef<HTMLTableElement>(null);

  return (
    <div className="relative my-2 overflow-x-auto rounded-lg border">
      <div className="flex justify-end border-b bg-muted/40 px-2 py-1">
        <CopyTableButton
          onCopy={() => (ref.current ? htmlTableToTsv(ref.current) : "")}
        />
      </div>
      <table ref={ref} {...props} className="w-full border-collapse text-sm">
        {children}
      </table>
    </div>
  );
}

export function copyStructuredTable(columns: string[], rows: string[][]): string {
  return tableToTsv(columns, rows);
}
