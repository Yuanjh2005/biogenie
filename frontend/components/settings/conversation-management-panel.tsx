"use client";

import { useDeferredValue, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { MessageSquareText, Search } from "lucide-react";
import { adminApi } from "@/lib/api/admin";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Markdown } from "@/components/chat/markdown";
import { ADMIN_SETTINGS_COPY } from "@/lib/i18n/admin-settings";
import { useLocaleStore } from "@/stores/locale";
import { cn } from "@/lib/utils";
import type { AdminConversationSessionDTO } from "@/types/api";

const PAGE_SIZE = 10;

function dateTime(value: string | undefined, locale: "zh-CN" | "en") {
  if (!value) return "-";
  const date = new Date(value.replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString(locale);
}

export function ConversationManagementPanel() {
  const locale = useLocaleStore((state) => state.locale);
  const copy = ADMIN_SETTINGS_COPY[locale];
  const [keyword, setKeyword] = useState("");
  const deferredKeyword = useDeferredValue(keyword.trim());
  const [currentPage, setCurrentPage] = useState(1);
  const [selected, setSelected] = useState<AdminConversationSessionDTO | null>(null);

  const sessions = useQuery({
    queryKey: ["admin-conversations", currentPage, deferredKeyword],
    queryFn: () => adminApi.conversations(currentPage, PAGE_SIZE, deferredKeyword || undefined),
  });
  const messages = useQuery({
    queryKey: ["admin-conversation-messages", selected?.sessionId],
    queryFn: () => adminApi.conversationMessages(selected!.sessionId),
    enabled: Boolean(selected),
  });

  const records = sessions.data?.records ?? [];
  const totalPages = Math.max(1, Math.ceil((sessions.data?.total ?? 0) / PAGE_SIZE));
  const accountName = (record: AdminConversationSessionDTO) =>
    record.userPhone || record.userNickname || copy.unknownUser(record.userId);

  return (
    <div>
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="shrink-0 text-sm font-medium">{copy.conversationsTitle}</h3>
        <div className="relative w-64 max-w-full shrink-0">
          <Search className="absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setCurrentPage(1);
            }}
            placeholder={copy.searchConversations}
            className="h-8 pl-8 text-xs"
          />
        </div>
      </div>

      <div className="max-h-[500px] overflow-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="sticky top-0 z-10 bg-background">
            <tr className="border-b bg-muted/40 text-xs text-muted-foreground">
              <th className="px-3 py-2 text-left font-medium">{copy.conversationAccount}</th>
              <th className="px-3 py-2 text-left font-medium">{copy.conversationTitle}</th>
              <th className="px-3 py-2 text-left font-medium">{copy.conversationApplication}</th>
              <th className="px-3 py-2 text-left font-medium">{copy.conversationStatus}</th>
              <th className="px-3 py-2 text-left font-medium">{copy.conversationUpdatedAt}</th>
              <th className="px-3 py-2 text-right font-medium" />
            </tr>
          </thead>
          <tbody>
            {sessions.isLoading ? (
              Array.from({ length: 5 }).map((_, index) => (
                <tr key={index} className="border-b last:border-0">
                  <td className="px-3 py-3" colSpan={6}><Skeleton className="h-4 w-full" /></td>
                </tr>
              ))
            ) : sessions.isError ? (
              <tr><td className="px-3 py-8 text-center text-xs text-muted-foreground" colSpan={6}>{copy.conversationsLoadFailed}</td></tr>
            ) : records.length === 0 ? (
              <tr><td className="px-3 py-8 text-center text-xs text-muted-foreground" colSpan={6}>{copy.noConversations}</td></tr>
            ) : records.map((record) => (
              <tr key={record.sessionId} className="border-b last:border-0 hover:bg-muted/20">
                <td className="max-w-32 px-3 py-2.5">
                  <div className="truncate" title={record.userNickname}>{accountName(record)}</div>
                  <div className="text-[11px] text-muted-foreground">#{record.userId}</div>
                </td>
                <td className="max-w-52 px-3 py-2.5"><div className="truncate" title={record.title}>{record.title || "-"}</div></td>
                <td className="max-w-32 px-3 py-2.5 text-xs"><div className="truncate" title={record.appKey}>{record.appKey || "-"}</div></td>
                <td className="px-3 py-2.5 text-xs">
                  <span className="inline-flex items-center gap-1.5 whitespace-nowrap">
                    <span className={cn("size-1.5 rounded-full", record.status === 1 ? "bg-emerald-500" : "bg-muted-foreground")} />
                    {record.status === 1 ? copy.conversationActive : copy.conversationArchived}
                  </span>
                </td>
                <td className="whitespace-nowrap px-3 py-2.5 text-xs text-muted-foreground">{dateTime(record.updateTime, locale)}</td>
                <td className="px-3 py-2.5 text-right">
                  <button type="button" className="text-xs text-primary hover:underline" onClick={() => setSelected(record)}>{copy.viewConversation}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
        <span>{copy.conversationPagination(sessions.data?.total ?? 0, PAGE_SIZE)}</span>
        <div className="flex items-center gap-2">
          <button type="button" className="rounded-md border px-2.5 py-1.5 text-foreground disabled:opacity-40" disabled={currentPage <= 1 || sessions.isLoading} onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}>{copy.previous}</button>
          <span>{currentPage} / {totalPages}</span>
          <button type="button" className="rounded-md border px-2.5 py-1.5 text-foreground disabled:opacity-40" disabled={currentPage >= totalPages || sessions.isLoading} onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}>{copy.next}</button>
        </div>
      </div>

      <Sheet open={Boolean(selected)} onOpenChange={(open) => { if (!open) setSelected(null); }}>
        <SheetContent className="w-[min(720px,calc(100vw-1rem))] sm:max-w-2xl">
          <SheetHeader className="border-b pr-12">
            <SheetTitle>{selected?.title || copy.conversationDetail}</SheetTitle>
            <SheetDescription>
              {selected ? `${accountName(selected)} · #${selected.userId} · ${dateTime(selected.updateTime, locale)}` : ""}
            </SheetDescription>
          </SheetHeader>
          <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-5">
            <div className="mb-3 flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <MessageSquareText className="size-4" /> {copy.conversationMessages}
            </div>
            {messages.isLoading ? (
              <div className="space-y-4">{Array.from({ length: 3 }).map((_, index) => <Skeleton key={index} className="h-24 w-full" />)}</div>
            ) : messages.isError ? (
              <div className="py-10 text-center text-sm text-muted-foreground">{copy.messagesLoadFailed}</div>
            ) : !messages.data?.length ? (
              <div className="py-10 text-center text-sm text-muted-foreground">{copy.noMessages}</div>
            ) : (
              <div className="space-y-5">
                {messages.data.map((message) => (
                  <div key={message.id} className="space-y-3 border-b pb-5 last:border-0">
                    <div className="ml-auto max-w-[88%] rounded-xl bg-primary px-3.5 py-2.5 text-primary-foreground">
                      <div className="mb-1 text-[11px] opacity-70">{copy.userQuestion}</div>
                      <div className="whitespace-pre-wrap break-words text-sm">{message.question}</div>
                    </div>
                    <div className="max-w-[94%] rounded-xl bg-muted px-3.5 py-2.5">
                      <div className="mb-1 text-[11px] text-muted-foreground">{copy.assistantAnswer}</div>
                      <Markdown content={message.answer} className="break-words text-sm" />
                    </div>
                    <div className="text-right text-[11px] text-muted-foreground">{dateTime(message.createTime, locale)}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
