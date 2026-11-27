"use client";

import { ExternalLink, GitFork, LineChart } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { citationAnchorId } from "@/lib/chat/citations";
import { pubmedSearchUrl, splitPaperCitation } from "@/lib/chat/papers";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import type { DocReference } from "@/types/chat";

export type PaperAction = "original" | "mindmap" | "insight";

/** 引用卡片列表；卡片同时作为正文 [n] 引用的跳转目标。 */
export function DocReferences({
  refs,
  citeScope,
  onPaperAction,
}: {
  refs?: DocReference[];
  citeScope?: string;
  onPaperAction?: (action: PaperAction, paper: DocReference) => void;
}) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  if (!refs || refs.length === 0) return null;

  return (
    <div className="mt-3 space-y-2">
      {refs.map((paper, i) => {
        const index = paper.index ?? i + 1;
        const citation = splitPaperCitation(paper);
        const originalUrl = paper.url || pubmedSearchUrl(paper.title);
        return (
          <Card
            key={paper.id ?? `${index}-${paper.title}`}
            id={citeScope ? citationAnchorId(citeScope, index) : undefined}
            className="gap-1 scroll-mt-4 p-3"
          >
            <div className="flex items-start gap-2">
              <span className="mt-0.5 shrink-0 text-xs font-medium text-muted-foreground">
                [{index}]
              </span>
              <div className="min-w-0 flex-1">
                <div className="text-sm font-medium">
                  {citation.before}
                  {citation.journal ? <em>{citation.journal}</em> : null}
                  {citation.after}
                </div>
                {paper.conclusion && (
                  <p className="mt-1 text-sm text-muted-foreground">{paper.conclusion}</p>
                )}
                <div className="mt-2 flex flex-wrap gap-1.5">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => {
                      if (onPaperAction) onPaperAction("original", paper);
                      else window.open(originalUrl, "_blank", "noopener,noreferrer");
                    }}
                  >
                    <ExternalLink className="size-3" />
                    {copy.viewOriginal}
                  </Button>
                  {onPaperAction && (
                    <>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="h-7 px-2 text-xs"
                        onClick={() => onPaperAction("mindmap", paper)}
                      >
                        <GitFork className="size-3" />
                        {copy.mindMap}
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="h-7 px-2 text-xs"
                        onClick={() => onPaperAction("insight", paper)}
                      >
                        <LineChart className="size-3" />
                        {copy.dataInsight}
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </div>
          </Card>
        );
      })}
    </div>
  );
}
