"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { Loader2, Paperclip } from "lucide-react";
import { conversationApi } from "@/lib/api/conversations";
import { ApiError } from "@/lib/api/errors";
import { Markdown } from "@/components/chat/markdown";
import { SHARE_COPY } from "@/lib/i18n/share";
import { useLocaleStore } from "@/stores/locale";
import type { ConversationShareViewDTO } from "@/types/api";

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; data: ConversationShareViewDTO };

function ShareShell({ children }: { children: React.ReactNode }) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHARE_COPY[locale];
  return (
    <div className="min-h-full bg-[linear-gradient(180deg,#F5F7FF_0%,#FFFFFF_40%)]">
      <header className="border-b bg-white/80 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-4xl items-center justify-between px-4">
          <Link href="/home" className="text-sm font-semibold text-[#4841F1]">
            BioGenie
          </Link>
          <span className="text-xs text-muted-foreground">{copy.readonlyHint}</span>
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-4 py-8">{children}</main>
    </div>
  );
}

function ShareError({ message }: { message: string }) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHARE_COPY[locale];
  return (
    <div className="rounded-2xl border bg-white px-6 py-12 text-center">
      <h1 className="text-lg font-semibold">{copy.cannotView}</h1>
      <p className="mt-2 text-sm text-muted-foreground">{message}</p>
      <Link
        href="/home"
        className="mt-6 inline-flex rounded-lg bg-brand px-4 py-2 text-sm text-brand-foreground"
      >
        {copy.backHome}
      </Link>
    </div>
  );
}

export default function SharePage() {
  const params = useParams<{ token: string }>();
  const token = typeof params.token === "string" ? params.token.trim() : "";
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHARE_COPY[locale];

  useEffect(() => {
    if (!token) return;

    let cancelled = false;
    void conversationApi
      .getShare(token)
      .then((view) => {
        if (!cancelled) setState({ status: "ready", data: view });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const fallback = SHARE_COPY[useLocaleStore.getState().locale].openFailed;
        const message = e instanceof ApiError ? e.message || fallback : fallback;
        setState({ status: "error", message });
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  if (!token) {
    return (
      <ShareShell>
        <ShareError message={copy.invalidLink} />
      </ShareShell>
    );
  }

  return (
    <ShareShell>
      {state.status === "loading" && (
        <div className="flex items-center justify-center gap-2 py-20 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          {copy.loading}
        </div>
      )}

      {state.status === "error" && <ShareError message={state.message} />}

      {state.status === "ready" && (
        <>
          <div className="mb-6">
            <h1 className="text-xl font-semibold text-[#0F172A]">
              {state.data.title || copy.fallbackTitle}
            </h1>
            {state.data.expireAt && (
              <p className="mt-1 text-xs text-muted-foreground">
                {copy.expiresAt(formatExpireAt(state.data.expireAt, locale))}
              </p>
            )}
          </div>

          <div className="space-y-5">
            {(state.data.messages ?? []).map((turn, index) => (
              <div key={`${turn.createTime ?? index}-${index}`} className="space-y-3">
                <div className="flex justify-end">
                  <div className="max-w-[80%] space-y-1">
                    {turn.attachments && turn.attachments.length > 0 && (
                      <div className="flex flex-wrap justify-end gap-1">
                        {turn.attachments.map((a, i) => (
                          <span
                            key={`${a.name ?? "att"}-${i}`}
                            className="inline-flex items-center gap-1 rounded-md bg-[#EAEDFE] px-2 py-1 text-xs"
                          >
                            <Paperclip className="size-3" />
                            {a.name || copy.attachment}
                          </span>
                        ))}
                      </div>
                    )}
                    <div className="whitespace-pre-wrap rounded-2xl rounded-tr-sm bg-chat-user-bubble px-4 py-2.5 text-sm text-chat-user-bubble-foreground">
                      {turn.question}
                    </div>
                  </div>
                </div>
                <div className="flex justify-start">
                  <div className="w-full min-w-0 rounded-2xl rounded-tl-sm bg-muted/50 px-4 py-3">
                    <Markdown content={turn.answer || ""} citeScope={`share-${index}`} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </ShareShell>
  );
}

function formatExpireAt(value: string, locale: "zh-CN" | "en"): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString(locale === "en" ? "en-US" : "zh-CN", { hour12: false });
}
