"use client";

import { useRouter } from "next/navigation";
import { ArrowRight } from "lucide-react";
import { InputBar, type InputSubmit } from "@/components/chat/input-bar";
import { MessageList } from "@/components/chat/message-list";
import { HotQuestions } from "@/components/chat/hot-questions";
import { MODULE_LIST, MODULES } from "@/lib/config/agents";
import { HOME_COPY } from "@/lib/i18n/home";
import { useChatStore } from "@/stores/chat";
import { useLocaleStore } from "@/stores/locale";
import type { ChatMessage, ModuleKey } from "@/types/chat";

const HOME_MODULE: ModuleKey = "smart-drgf";
const EN_HOME_QUESTIONS = [
  "3D structures and PTM information for FGF1 and FGF2",
  "Human FGF family members and their sequences",
  "VEGF-related patents",
];

const EMPTY_MESSAGES: ChatMessage[] = [];

interface ModuleCardMeta {
  iconSrc: string;
  illustrationSrc: string;
  gradient: string;
  borderColor: string;
  buttonColor: string;
}

const MODULE_CARDS: Record<ModuleKey, ModuleCardMeta> = {
  "smart-drgf": {
    iconSrc: "/images/home/smart-drgf-icon.svg",
    illustrationSrc: "/images/home/smart-drgf-illustration.svg",
    gradient: "linear-gradient(180deg, #EDFFF4 0%, #FFF 50%)",
    borderColor: "#D6F5E1",
    buttonColor: "#16A34A",
  },
  "deep-pubmed": {
    iconSrc: "/images/home/deep-pubmed-icon.svg",
    illustrationSrc: "/images/home/deep-pubmed-illustration.svg",
    gradient: "linear-gradient(180deg, #E1EBFF 0%, #FFF 50%)",
    borderColor: "#DCE6FB",
    buttonColor: "#2563EB",
  },
  "design-mcp": {
    iconSrc: "/images/home/design-mcp-icon.svg",
    illustrationSrc: "/images/home/design-mcp-illustration.svg",
    gradient: "linear-gradient(180deg, #F6EDFF 0%, #FFF 50%)",
    borderColor: "#EBDFF9",
    buttonColor: "#7C3AED",
  },
};

export default function HomePage() {
  const router = useRouter();
  const locale = useLocaleStore((s) => s.locale);
  const copy = HOME_COPY[locale];

  const sendMessage = useChatStore((s) => s.sendMessage);
  const stopGenerating = useChatStore((s) => s.stopGenerating);
  const mode = useChatStore((s) => s.modeByModule[HOME_MODULE]);
  const setMode = useChatStore((s) => s.setMode);
  const activeId = useChatStore((s) => s.activeIdByModule[HOME_MODULE]);
  const streaming = useChatStore((s) => {
    const id = s.activeIdByModule[HOME_MODULE];
    return Boolean(id && s.streamingConversationIds[id]);
  });
  const sessionId = useChatStore((s) => {
    const id = s.activeIdByModule[HOME_MODULE];
    return s.conversations.find((c) => c.id === id)?.sessionId;
  });
  const messages =
    useChatStore((s) => (activeId ? s.messagesByConversation[activeId] : undefined)) ??
    EMPTY_MESSAGES;

  // 仅当有「当前激活」会话时进入对话态；点首页会清空 active，回到欢迎页
  const hasMessages = Boolean(activeId) && messages.length > 0;

  const submit = (text: string, extras: InputSubmit = {}) => {
    const q = text.trim();
    if (!q) return;
    void sendMessage(HOME_MODULE, q, extras);
  };

  const regenerate = (assistantIndex: number) => {
    const prev = messages[assistantIndex - 1];
    if (prev?.role === "user") {
      void sendMessage(HOME_MODULE, prev.content, {
        attachments: prev.attachments?.filter((a) => a.status === "done" && a.url),
      });
    }
  };

  if (hasMessages) {
    return (
      <div className="flex h-full flex-col">
        <div className="min-h-0 flex-1 overflow-y-auto">
          <MessageList
            messages={messages}
            streamingId={streaming ? activeId ?? null : null}
            onRegenerate={regenerate}
            onEdit={(message, content, attachments) =>
              void sendMessage(HOME_MODULE, content, {
                attachments,
                replaceMessageId: message.id,
              })
            }
            sessionId={sessionId}
          />
        </div>
        <InputBar
          onSubmit={submit}
          streaming={streaming}
          onStop={() => stopGenerating(activeId)}
          allowAttachments
          uploadPrefix={HOME_MODULE}
          placeholder={copy.placeholder}
          modeSelect={{
            modes: MODULES[HOME_MODULE].modes,
            value: mode,
            onChange: (m) => setMode(HOME_MODULE, m),
          }}
        />
      </div>
    );
  }

  return (
    <div className="relative flex h-full flex-col overflow-y-auto">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 -z-10 bg-[url('/images/home-bg.svg')] bg-cover bg-center bg-no-repeat"
      />
      <div className="relative z-10 mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center px-6 py-10">
        <div className="mb-8 text-center">
          <h1
            className="bg-clip-text text-transparent"
            style={{
              backgroundImage: "linear-gradient(90deg, #4841F1 0%, #5D8BFF 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              fontSize: "60px",
              fontWeight: 700,
              lineHeight: 1.2,
            }}
          >
            BioGenie
          </h1>
          <h2 className="mt-1 text-[36px] font-semibold text-[#1E2939]">
            {copy.title}
          </h2>
          <p className="mt-2 text-base font-normal text-[#64748B]">
            {copy.subtitle}
          </p>
        </div>

        <InputBar
          onSubmit={submit}
          streaming={streaming}
          onStop={() => stopGenerating(activeId)}
          allowAttachments
          uploadPrefix={HOME_MODULE}
          placeholder={copy.placeholder}
        />

        <div className="mt-4">
          <HotQuestions
            module={HOME_MODULE}
            mode={mode}
            questionsOverride={locale === "en" ? EN_HOME_QUESTIONS : undefined}
            onSelect={submit}
          />
        </div>

        <div className="mt-10 grid gap-4 sm:grid-cols-3">
          {MODULE_LIST.map((m) => {
            const card = MODULE_CARDS[m.key];
            const cardCopy = copy.modules[m.key];
            return (
              <div
                key={m.key}
                className="relative flex flex-col overflow-hidden rounded-2xl border p-5"
                style={{ background: card.gradient, borderColor: card.borderColor }}
              >
                <div className="relative z-10 flex items-start gap-3">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-white shadow-sm">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={card.iconSrc} alt="" className="size-10 rounded-lg" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="font-semibold">{m.label}</h3>
                    <p className="text-xs italic text-muted-foreground">{cardCopy.description}</p>
                  </div>
                </div>

                <ul className="relative z-10 mt-4 flex-1 space-y-1.5 text-sm text-foreground/80">
                  {cardCopy.bullets.map((bullet) => (
                    <li key={bullet} className="flex items-start gap-2">
                      <span className="mt-[0.55em] size-1 shrink-0 rounded-full bg-current" />
                      <span>{bullet}</span>
                    </li>
                  ))}
                </ul>

                <button
                  onClick={() => router.push(m.route)}
                  className="relative z-10 mt-5 inline-flex w-fit items-center gap-1 rounded-lg px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90"
                  style={{ backgroundColor: card.buttonColor }}
                >
                  {cardCopy.buttonLabel}
                  <ArrowRight className="size-3.5" />
                </button>

                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={card.illustrationSrc}
                  alt=""
                  aria-hidden
                  className="pointer-events-none absolute -bottom-2 -right-2 z-0 h-auto w-28 select-none"
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
