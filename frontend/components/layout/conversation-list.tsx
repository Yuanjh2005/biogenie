"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { FileSearch, MessageSquare, RefreshCw, Search, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import { SHELL_COPY } from "@/lib/i18n/shell";
import { useChatStore } from "@/stores/chat";
import { groupConversations } from "@/lib/conversations";
import { moduleFromPath, routeForModule } from "@/lib/config/agents";
import { useAuthStore } from "@/stores/auth";
import { useLocaleStore } from "@/stores/locale";
import type { Conversation } from "@/types/chat";

export function ConversationList({ onNavigate }: { onNavigate?: () => void }) {
  const router = useRouter();
  const pathname = usePathname();
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHELL_COPY[locale];
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const conversations = useChatStore((s) => s.conversations);
  const conversationsLoaded = useChatStore((s) => s.conversationsLoaded);
  const conversationsLoading = useChatStore((s) => s.conversationsLoading);
  const conversationsLoadFailed = useChatStore((s) => s.conversationsLoadFailed);
  const conversationsPage = useChatStore((s) => s.conversationsPage);
  const conversationsTotal = useChatStore((s) => s.conversationsTotal);
  const conversationPageIds = useChatStore((s) => s.conversationPageIds);
  const conversationsKeyword = useChatStore((s) => s.conversationsKeyword);
  const activeIdByModule = useChatStore((s) => s.activeIdByModule);
  const loadConversations = useChatStore((s) => s.loadConversations);
  const selectConversation = useChatStore((s) => s.selectConversation);
  const deleteConversation = useChatStore((s) => s.deleteConversation);
  const loadMoreRef = useRef<HTMLDivElement>(null);
  const failedSearchRef = useRef<string | null>(null);
  const [searchValue, setSearchValue] = useState("");
  const normalizedSearch = searchValue.trim();
  const isSearchPending = normalizedSearch !== conversationsKeyword;

  useEffect(() => {
    if (isAuthenticated && !conversationsLoaded) {
      void loadConversations();
    }
  }, [isAuthenticated, conversationsLoaded, loadConversations]);

  useEffect(() => {
    if (
      !isAuthenticated ||
      !conversationsLoaded ||
      !isSearchPending ||
      conversationsLoading ||
      (conversationsLoadFailed && failedSearchRef.current === normalizedSearch)
    ) {
      return;
    }
    const timer = window.setTimeout(() => {
      failedSearchRef.current = normalizedSearch;
      void loadConversations(1, normalizedSearch);
    }, 250);
    return () => window.clearTimeout(timer);
  }, [
    conversationsLoaded,
    conversationsLoading,
    conversationsKeyword,
    conversationsLoadFailed,
    isAuthenticated,
    isSearchPending,
    loadConversations,
    normalizedSearch,
  ]);

  const visibleConversationIds = new Set(conversationPageIds);
  const visibleConversations = conversations.filter(
    (conversation) =>
      visibleConversationIds.has(conversation.id) ||
      (!conversation.sessionId &&
        conversation.title.toLocaleLowerCase().includes(conversationsKeyword.toLocaleLowerCase())),
  );
  const groups = groupConversations(visibleConversations);
  const currentModule = moduleFromPath(pathname);
  // 只高亮当前模块下的活跃会话，避免多模块同时显示选中
  const activeId = currentModule ? activeIdByModule[currentModule] : undefined;
  const groupLabels: { key: "today" | "yesterday" | "earlier"; label: string }[] = [
    { key: "today", label: copy.today },
    { key: "yesterday", label: copy.yesterday },
    { key: "earlier", label: copy.earlier },
  ];
  const hasMore = conversationPageIds.length < conversationsTotal;

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target || !hasMore || conversationsLoading || conversationsLoadFailed || isSearchPending) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          void loadConversations(conversationsPage + 1, conversationsKeyword);
        }
      },
      { rootMargin: "80px 0px" },
    );
    observer.observe(target);
    return () => observer.disconnect();
  }, [
    hasMore,
    conversationsLoading,
    conversationsLoadFailed,
    conversationsPage,
    conversationsKeyword,
    loadConversations,
    isSearchPending,
  ]);

  const handleSelect = (conv: Conversation) => {
    void selectConversation(conv.id);
    router.push(routeForModule(conv.module, pathname));
    onNavigate?.();
  };

  if (!conversationsLoaded) {
    return (
      <div className="flex-1 px-4 py-6 text-center text-xs text-muted-foreground">
        {copy.loading}
      </div>
    );
  }

  if (conversations.length === 0 && !conversationsKeyword && !normalizedSearch) {
    if (conversationsLoadFailed) {
      return (
        <div className="flex flex-1 flex-col items-center gap-2 px-4 py-6 text-center text-xs text-muted-foreground">
          <span>{copy.conversationsLoadFailed}</span>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            disabled={conversationsLoading}
            onClick={() => void loadConversations()}
          >
            <RefreshCw className={cn("size-3.5", conversationsLoading && "animate-spin")} />
            {copy.retry}
          </Button>
        </div>
      );
    }
    return (
      <div className="flex-1 px-4 py-6 text-center text-xs text-muted-foreground">
        {copy.noConversations}
      </div>
    );
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col px-3 pb-2">
      <div className="mb-2 flex items-center justify-between px-1">
        <span className="text-xs font-semibold tracking-wide text-foreground/75">{copy.conversationHistory}</span>
        {conversationsKeyword && !isSearchPending && (
          <span className="text-[11px] text-muted-foreground">{conversationsTotal}</span>
        )}
      </div>
      <div className="relative mb-2">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={searchValue}
          onChange={(event) => setSearchValue(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Escape") setSearchValue("");
          }}
          placeholder={copy.searchConversations}
          aria-label={copy.searchConversations}
          className="h-9 rounded-xl border-transparent bg-muted/70 pr-8 pl-8 text-sm shadow-none transition-[background-color,box-shadow] placeholder:text-muted-foreground/80 hover:bg-muted focus-visible:border-brand/30 focus-visible:bg-background focus-visible:ring-2 focus-visible:ring-brand/20"
        />
        {searchValue && (
          <button
            type="button"
            onClick={() => setSearchValue("")}
            className="absolute top-1/2 right-2 flex size-5 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-background hover:text-foreground"
            aria-label={copy.clearSearch}
          >
            <X className="size-3.5" />
          </button>
        )}
      </div>
      <ScrollArea className="min-h-0 flex-1">
        <div className="space-y-4 py-1">
          {conversationsLoadFailed && isSearchPending ? (
            <div className="flex flex-col items-center gap-2 py-8 text-center text-xs text-muted-foreground">
              <span>{copy.conversationsLoadFailed}</span>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                onClick={() => {
                  failedSearchRef.current = normalizedSearch;
                  void loadConversations(1, normalizedSearch);
                }}
              >
                <RefreshCw className="size-3.5" />
                {copy.retry}
              </Button>
            </div>
          ) : isSearchPending || (conversationsLoading && Boolean(normalizedSearch)) ? (
            <div className="py-8 text-center text-xs text-muted-foreground">{copy.loading}</div>
          ) : visibleConversations.length === 0 && conversationsKeyword ? (
            <div className="flex flex-col items-center px-3 py-10 text-center text-muted-foreground">
              <div className="mb-3 rounded-full bg-muted p-2.5">
                <FileSearch className="size-4" />
              </div>
              <p className="text-xs leading-5">{copy.noSearchResults(conversationsKeyword)}</p>
              <button
                type="button"
                onClick={() => setSearchValue("")}
                className="mt-2 text-xs font-medium text-brand hover:underline"
              >
                {copy.clearSearch}
              </button>
            </div>
          ) : groupLabels.map(({ key, label }) => {
          const list = groups[key];
          if (list.length === 0) return null;
          return (
            <div key={key}>
              <div className="px-2 py-1 text-[11px] font-semibold tracking-wide text-muted-foreground">
                {label}
              </div>
              <ul className="space-y-0.5">
                {list.map((conv) => (
                  <li key={conv.id}>
                    <button
                      onClick={() => handleSelect(conv)}
                      className={cn(
                        "group flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-left text-sm transition-colors hover:bg-accent",
                        activeId === conv.id && "bg-[#EFF3FD] font-medium text-[#3e42d5] hover:bg-[#EFF3FD]",
                      )}
                    >
                      <MessageSquare className={cn("size-4 shrink-0 text-muted-foreground", activeId === conv.id && "text-[#4C4FF4]")} />
                      <span className="flex-1 truncate">{conv.title}</span>
                      <Trash2
                        className="size-3.5 shrink-0 text-muted-foreground opacity-0 hover:text-destructive group-hover:opacity-100"
                        onClick={(e) => {
                          e.stopPropagation();
                          void deleteConversation(conv.id);
                        }}
                        aria-label={copy.deleteConversation}
                      />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          );
          })}
        {hasMore && !isSearchPending && <div ref={loadMoreRef} className="h-px" aria-hidden="true" />}
        {conversationsLoading && conversationsLoaded && !normalizedSearch && (
          <div className="py-2 text-center text-xs text-muted-foreground">{copy.loading}</div>
        )}
        {hasMore && conversationsLoadFailed && (
          <div className="flex justify-center border-t pt-2">
            <Button type="button" size="sm" variant="ghost" onClick={() => void loadConversations(conversationsPage + 1, conversationsKeyword)}>
              <RefreshCw className="size-3.5" />
              {copy.retry}
            </Button>
          </div>
        )}
        </div>
      </ScrollArea>
    </div>
  );
}
