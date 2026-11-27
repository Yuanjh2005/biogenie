"use client";

import { useState } from "react";
import { ArrowUp, Loader2, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Markdown } from "@/components/chat/markdown";
import { Thoughts } from "@/components/chat/thoughts";
import { DocReferences, type PaperAction } from "@/components/chat/doc-references";
import { StructuredTableView } from "@/components/chat/structured-table";
import { MessageActions } from "@/components/chat/message-actions";
import { UserMessageActions } from "@/components/chat/user-message-actions";
import { FileChip } from "@/components/chat/file-chip";
import {
  extractPapersFromContent,
  mergePapers,
  stripBibliographyFromContent,
} from "@/lib/chat/papers";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import type { Attachment, ChatMessage, DocReference } from "@/types/chat";

interface MessageBubbleProps {
  message: ChatMessage;
  question: string;
  streaming: boolean;
  onRegenerate?: () => void;
  onEdit?: (content: string, attachments: Attachment[]) => void;
  editDisabled?: boolean;
  sessionId?: string;
  onPaperAction?: (action: PaperAction, paper: DocReference) => void;
}

export function MessageBubble({
  message,
  question,
  streaming,
  onRegenerate,
  onEdit,
  editDisabled,
  sessionId,
  onPaperAction,
}: MessageBubbleProps) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const [editing, setEditing] = useState(false);
  const [editValue, setEditValue] = useState(message.content);
  const [editAttachments, setEditAttachments] = useState(message.attachments ?? []);

  if (message.role === "user") {
    const cancelEdit = () => {
      setEditValue(message.content);
      setEditAttachments(message.attachments ?? []);
      setEditing(false);
    };
    const submitEdit = () => {
      const content = editValue.trim();
      if (!content || !onEdit || editDisabled) return;
      onEdit(content, editAttachments);
      setEditing(false);
    };

    return (
      <div className="flex justify-end">
        <div className="max-w-[80%] space-y-1">
          {editAttachments.length > 0 && (
            <div className="flex flex-wrap justify-end gap-2">
              {editAttachments.map((a) => (
                <FileChip
                  key={a.id}
                  attachment={a}
                  onRemove={editing ? () => setEditAttachments((items) => items.filter((item) => item.id !== a.id)) : undefined}
                />
              ))}
            </div>
          )}
          {editing ? (
            <div className="min-w-80 rounded-2xl rounded-tr-sm bg-chat-user-bubble p-2 text-chat-user-bubble-foreground">
              <Textarea
                autoFocus
                value={editValue}
                onChange={(event) => setEditValue(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.shiftKey) {
                    event.preventDefault();
                    submitEdit();
                  }
                  if (event.key === "Escape") cancelEdit();
                }}
                className="max-h-40 min-h-12 resize-none border-0 bg-transparent shadow-none focus-visible:ring-0"
              />
              <div className="flex justify-end gap-1 pt-1">
                <Button size="icon" variant="ghost" className="size-7" onClick={cancelEdit} aria-label={copy.cancel} title={copy.cancel}>
                  <X className="size-3.5" />
                </Button>
                <Button size="icon" className="size-7" onClick={submitEdit} disabled={!editValue.trim()} aria-label={copy.send} title={copy.send}>
                  <ArrowUp className="size-3.5" />
                </Button>
              </div>
            </div>
          ) : (
            <>
              <div className="whitespace-pre-wrap rounded-2xl rounded-tr-sm bg-chat-user-bubble px-4 py-2.5 text-sm text-chat-user-bubble-foreground">
                {message.content}
              </div>
              <UserMessageActions
                message={message}
                onEdit={onEdit ? () => setEditing(true) : undefined}
                editDisabled={editDisabled}
              />
            </>
          )}
        </div>
      </div>
    );
  }

  const isPending = message.status === "pending";
  const isError = message.status === "error";
  const isStopped = message.status === "stopped";
  const extractedPapers = streaming ? undefined : extractPapersFromContent(message.content);
  const papers = mergePapers(message.docReferences, extractedPapers);
  const citationUrls =
    message.mode === "web"
      ? Object.fromEntries(
          papers
            .filter((paper) => paper.index && paper.url)
            .map((paper) => [String(paper.index), paper.url as string]),
        )
      : undefined;
  const renderedContent =
    papers.length > 0 && !streaming
      ? stripBibliographyFromContent(message.content)
      : message.content;
  const hasBody =
    Boolean(renderedContent.trim()) ||
    Boolean(message.table?.rows.length) ||
    papers.length > 0;

  return (
    <div className="flex justify-start">
      <div className="w-full min-w-0">
        {message.thoughts && (
          <Thoughts
            content={message.thoughts}
            defaultOpen={isStopped && !message.content?.trim()}
          />
        )}

        {message.retrievalSummary && (
          <div className="mb-2 text-sm text-muted-foreground">
            {message.retrievalSummary}
          </div>
        )}

        {(isPending || streaming) && !hasBody ? (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" /> {copy.thinking}
          </div>
        ) : isError ? (
          <div className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {copy.generateFailed}
          </div>
        ) : hasBody ? (
          <div
            className={cn(
              "rounded-2xl rounded-tl-sm bg-muted/50 px-4 py-3",
              streaming && "after:ml-0.5 after:inline-block after:animate-pulse after:content-['▋']",
            )}
          >
            <Markdown
              content={renderedContent}
              citeScope={message.id}
              citationUrls={citationUrls}
            />
            <StructuredTableView table={message.table} />
            <DocReferences
              refs={papers}
              citeScope={message.id}
              onPaperAction={onPaperAction}
            />
          </div>
        ) : null}

        {isStopped && (
          <div className="mt-1 text-xs text-muted-foreground">{copy.stopped}</div>
        )}

        {!streaming && !isPending && !isError && (
          <MessageActions
            message={message}
            question={question}
            onRegenerate={onRegenerate}
            sessionId={sessionId}
          />
        )}
      </div>
    </div>
  );
}
