"use client";

import { useState } from "react";
import { Brain, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";

/** 专家模式思考过程折叠区 */
export function Thoughts({
  content,
  defaultOpen = false,
}: {
  content: string;
  defaultOpen?: boolean;
}) {
  const [userOpen, setUserOpen] = useState<boolean | null>(null);
  const open = userOpen ?? defaultOpen;
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  if (!content) return null;

  return (
    <div className="mb-2 rounded-lg border bg-muted/40 text-sm">
      <button
        className="flex w-full items-center gap-1.5 px-3 py-2 text-muted-foreground"
        onClick={() => setUserOpen((v) => !(v ?? defaultOpen))}
      >
        <Brain className="size-4" />
        <span>{copy.thoughts}</span>
        <ChevronDown className={cn("size-4 transition-transform", open && "rotate-180")} />
      </button>
      {open && (
        <p className="whitespace-pre-wrap px-3 pb-3 text-muted-foreground">{content}</p>
      )}
    </div>
  );
}
