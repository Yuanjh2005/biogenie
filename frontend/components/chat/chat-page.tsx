"use client";

import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ModeSwitch } from "@/components/chat/mode-switch";
import { MessageList } from "@/components/chat/message-list";
import { InputBar, type InputSubmit } from "@/components/chat/input-bar";
import { Welcome } from "@/components/chat/welcome";
import { HotQuestions } from "@/components/chat/hot-questions";
import type { PaperAction } from "@/components/chat/doc-references";
import { MODULES } from "@/lib/config/agents";
import type { DesignMode, PubmedArticleType, PubmedYear } from "@/lib/config/chat-filters";
import {
  dataInsightPrompt,
  mindMapPrompt,
  pubmedSearchUrl,
} from "@/lib/chat/papers";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useChatStore } from "@/stores/chat";
import { useLocaleStore } from "@/stores/locale";
import { CHAT_COLUMN_CLASS } from "@/lib/config/layout";
import { clearSsoLaunch, readSsoLaunch } from "@/lib/auth/sso-launch";
import type { ChatMessage, DocReference, ModuleKey } from "@/types/chat";

const EMPTY_MESSAGES: ChatMessage[] = [];

interface ChatPageProps {
  module: ModuleKey;
  allowAttachments?: boolean;
}

export function ChatPage({ module, allowAttachments }: ChatPageProps) {
  const config = MODULES[module];
  const searchParams = useSearchParams();
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];

  const mode = useChatStore((s) => s.modeByModule[module]);
  const setMode = useChatStore((s) => s.setMode);
  const sendMessage = useChatStore((s) => s.sendMessage);
  const stopGenerating = useChatStore((s) => s.stopGenerating);
  const activeId = useChatStore((s) => s.activeIdByModule[module]);
  const streaming = useChatStore((s) => {
    const id = s.activeIdByModule[module];
    return Boolean(id && s.streamingConversationIds[id]);
  });
  const sessionId = useChatStore((s) => {
    const id = s.activeIdByModule[module];
    return s.conversations.find((c) => c.id === id)?.sessionId;
  });
  const messages =
    useChatStore((s) => (activeId ? s.messagesByConversation[activeId] : undefined)) ??
    EMPTY_MESSAGES;
  const messagesLoading = useChatStore((s) => s.messagesLoadingId === activeId);

  const [designMode, setDesignMode] = useState<DesignMode>("rational");
  const [pubmedYear, setPubmedYear] = useState<PubmedYear>("unlimited");
  const [pubmedType, setPubmedType] = useState<PubmedArticleType>("unlimited");

  const inConversation = Boolean(activeId);
  const isDesign = module === "design-mcp";
  const isPubmed = module === "deep-pubmed";

  const extrasFromFilters = (): Pick<
    InputSubmit,
    "redesignOption" | "pubmedYear" | "pubmedArticleType"
  > => ({
    redesignOption: isDesign ? designMode : undefined,
    pubmedYear: isPubmed ? pubmedYear : undefined,
    pubmedArticleType: isPubmed ? pubmedType : undefined,
  });

  const send = (text: string, extras: InputSubmit) => {
    sendMessage(module, text, { ...extrasFromFilters(), ...extras });
  };

  // 首页 URL 或 SSO ticket 带入的问题自动发送一次。
  const autoSent = useRef(false);
  useEffect(() => {
    const urlQuestion = searchParams.get("q")?.trim();
    const launch = urlQuestion ? null : readSsoLaunch(module);
    const q = urlQuestion || launch?.question || (launch?.attachments?.length ? copy.attachmentPrompt : "");
    if (q && !autoSent.current && !streaming) {
      autoSent.current = true;
      clearSsoLaunch();
      sendMessage(module, q, { attachments: launch?.attachments });
      if (searchParams.has("q")) {
        window.history.replaceState(null, "", config.route);
      }
    }
  }, [searchParams, module, sendMessage, streaming, config.route, copy.attachmentPrompt]);

  const regenerate = (assistantIndex: number) => {
    const prev = messages[assistantIndex - 1];
    if (prev?.role === "user") {
      void sendMessage(module, prev.content, {
        attachments: prev.attachments?.filter((a) => a.status === "done" && a.url),
        ...extrasFromFilters(),
      });
    }
  };

  const onPaperAction = (action: PaperAction, paper: DocReference) => {
    if (action === "original") {
      window.open(paper.url || pubmedSearchUrl(paper.title), "_blank", "noopener,noreferrer");
      return;
    }
    const prompt =
      action === "mindmap" ? mindMapPrompt(paper, locale) : dataInsightPrompt(paper, locale);
    void sendMessage(module, prompt, extrasFromFilters());
  };

  const modeSelectProps = {
    modes: config.modes,
    value: mode,
    onChange: (m: typeof mode) => setMode(module, m),
  };

  const filterProps = {
    ...(isDesign ? { designMode, onDesignModeChange: setDesignMode } : {}),
    ...(isPubmed
      ? {
          pubmedYear,
          onPubmedYearChange: setPubmedYear,
          pubmedType,
          onPubmedTypeChange: setPubmedType,
        }
      : {}),
  };

  return (
    <div className="flex h-full flex-col">
      {inConversation ? (
        <>
          <div className="min-h-0 flex-1 overflow-y-auto">
            {messagesLoading && messages.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                {copy.loading}
              </div>
            ) : (
              <MessageList
                messages={messages}
                streamingId={streaming ? activeId ?? null : null}
                onRegenerate={regenerate}
                onEdit={(message, content, attachments) =>
                  void sendMessage(module, content, {
                    attachments,
                    replaceMessageId: message.id,
                    ...extrasFromFilters(),
                  })
                }
                sessionId={sessionId}
                onPaperAction={onPaperAction}
              />
            )}
          </div>
          <InputBar
            onSubmit={send}
            streaming={streaming}
            onStop={() => stopGenerating(activeId)}
            allowAttachments={allowAttachments}
            uploadPrefix={module}
            modeSelect={modeSelectProps}
            {...filterProps}
          />
        </>
      ) : (
        <div className="flex h-full flex-col items-center justify-center overflow-y-auto py-6">
          <div className={CHAT_COLUMN_CLASS}>
            <Welcome module={module} />
            <div className="mt-8 flex justify-center">
              <ModeSwitch
                modes={config.modes}
                value={mode}
                onChange={(m) => setMode(module, m)}
              />
            </div>
          </div>
          <div className="mt-6 w-full">
            <InputBar
              onSubmit={send}
              streaming={streaming}
              onStop={() => stopGenerating(activeId)}
              allowAttachments={allowAttachments}
              uploadPrefix={module}
              {...filterProps}
            />
          </div>
          <div className={`mt-4 ${CHAT_COLUMN_CLASS}`}>
            <HotQuestions
              module={module}
              mode={mode}
              onSelect={(q) => void sendMessage(module, q, extrasFromFilters())}
            />
          </div>
        </div>
      )}
    </div>
  );
}
