"use client";

import { useEffect, useState } from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { getAgent } from "@/lib/config/agents";
import { HOME_COPY } from "@/lib/i18n/home";
import { hotQuestionApi } from "@/lib/api/hot-questions";
import { useLocaleStore } from "@/stores/locale";
import type { ChatMode, ModuleKey } from "@/types/chat";

const HOT_QUESTION_MAX_CHARS = 15;
const CHIP_CLASS =
  "rounded-full bg-[#EAEDFE] px-3 py-1.5 text-xs text-[#0F172A] hover:opacity-80";

function truncateChars(text: string, max: number): string {
  const chars = Array.from(text);
  if (chars.length <= max) return text;
  return `${chars.slice(0, max).join("")}...`;
}

interface HotQuestionsProps {
  module: ModuleKey;
  mode?: ChatMode;
  /** 不传 app 时按模块当前模式解析；也可显式指定后端 app key */
  app?: string;
  limit?: number;
  questionsOverride?: string[];
  onSelect: (question: string) => void;
}

export function HotQuestions({
  module,
  mode = "fast",
  app,
  limit = 3,
  questionsOverride,
  onSelect,
}: HotQuestionsProps) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = HOME_COPY[locale];
  const fallback = copy.modules[module].recommended;
  const appKey = app ?? getAgent(module, mode)?.app;
  const requestKey = `${module}:${mode}:${appKey ?? ""}:${limit}:${locale}`;
  const [remoteQuestions, setRemoteQuestions] = useState<{
    key: string;
    questions: string[];
  } | null>(null);
  const questions = questionsOverride ??
    (remoteQuestions?.key === requestKey ? remoteQuestions.questions : fallback);

  useEffect(() => {
    if (locale === "en" || questionsOverride) return;

    let cancelled = false;

    void hotQuestionApi
      .list(limit, appKey)
      .then((list) => {
        if (cancelled) return;
        const fromApi = (list ?? [])
          .map((item) => item.question?.trim())
          .filter((q): q is string => Boolean(q));
        if (fromApi.length > 0) {
          setRemoteQuestions({ key: requestKey, questions: fromApi });
        }
      })
      .catch(() => {
        // 失败时保留静态兜底
      });

    return () => {
      cancelled = true;
    };
  }, [appKey, limit, locale, requestKey, questionsOverride]);

  if (questions.length === 0) return null;

  return (
    <TooltipProvider>
      <div className="flex flex-wrap items-center justify-center gap-2">
        <span className="text-xs text-muted-foreground">{copy.tryQuestions}</span>
        {questions.map((q) => {
          const label = truncateChars(q, HOT_QUESTION_MAX_CHARS);
          const truncated = label !== q;
          if (!truncated) {
            return (
              <button
                key={q}
                type="button"
                onClick={() => onSelect(q)}
                className={CHIP_CLASS}
              >
                {label}
              </button>
            );
          }
          return (
            <Tooltip key={q}>
              <TooltipTrigger
                type="button"
                onClick={() => onSelect(q)}
                className={CHIP_CLASS}
              >
                {label}
              </TooltipTrigger>
              <TooltipContent className="max-w-xs whitespace-normal break-words text-left">
                {q}
              </TooltipContent>
            </Tooltip>
          );
        })}
      </div>
    </TooltipProvider>
  );
}
