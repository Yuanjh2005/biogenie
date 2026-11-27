"use client";

import { useRef, useState } from "react";
import { ArrowUp, Paperclip } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FileChip } from "@/components/chat/file-chip";
import { ModeSelect } from "@/components/chat/mode-switch";
import {
  DESIGN_MODES,
  DESIGN_MODE_LABELS,
  PUBMED_TYPE_LABELS,
  PUBMED_TYPE_OPTIONS,
  PUBMED_YEAR_LABELS,
  PUBMED_YEAR_OPTIONS,
  type DesignMode,
  type PubmedArticleType,
  type PubmedYear,
} from "@/lib/config/chat-filters";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { env } from "@/lib/config/env";
import { CHAT_COLUMN_CLASS } from "@/lib/config/layout";
import { fileApi } from "@/lib/api/file";
import { useLocaleStore } from "@/stores/locale";
import { cn } from "@/lib/utils";
import type { Attachment, ChatMode } from "@/types/chat";

export interface InputSubmit {
  attachments?: Attachment[];
  redesignOption?: string;
  pubmedYear?: PubmedYear;
  pubmedArticleType?: PubmedArticleType;
}

export interface ModeSelectProps {
  modes: ChatMode[];
  value: ChatMode;
  onChange: (mode: ChatMode) => void;
}

interface InputBarProps {
  onSubmit: (text: string, extras: InputSubmit) => void;
  streaming: boolean;
  onStop: () => void;
  placeholder?: string;
  /** 启用文件上传 */
  allowAttachments?: boolean;
  acceptTypes?: string;
  uploadPrefix?: string;
  designMode?: DesignMode;
  onDesignModeChange?: (mode: DesignMode) => void;
  pubmedYear?: PubmedYear;
  onPubmedYearChange?: (year: PubmedYear) => void;
  pubmedType?: PubmedArticleType;
  onPubmedTypeChange?: (type: PubmedArticleType) => void;
  /** 对话中：输入框右侧模式下拉 */
  modeSelect?: ModeSelectProps;
  initialValue?: string;
}

export function InputBar({
  onSubmit,
  streaming,
  onStop,
  placeholder,
  allowAttachments = false,
  acceptTypes = ".doc,.docx,.pdf,.xls,.xlsx,.txt,.md,image/*",
  uploadPrefix = "chat",
  designMode,
  onDesignModeChange,
  pubmedYear,
  onPubmedYearChange,
  pubmedType,
  onPubmedTypeChange,
  modeSelect,
  initialValue = "",
}: InputBarProps) {
  const [value, setValue] = useState(initialValue);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const resolvedPlaceholder = placeholder ?? copy.placeholder;
  const yearLabels = PUBMED_YEAR_LABELS[locale];
  const typeLabels = PUBMED_TYPE_LABELS[locale];

  const shouldPackageText = value.length > env.autoTextAttachmentLength;
  const packagedTextBytes = shouldPackageText
    ? new Blob([value], { type: "text/plain;charset=utf-8" }).size
    : 0;
  const overLimit = packagedTextBytes > env.maxUploadBytes;
  const uploading = attachments.some((a) => a.status === "uploading");
  const hasReadyAttachment = attachments.some((a) => a.status === "done" && a.url);
  const canSend =
    (value.trim().length > 0 || hasReadyAttachment) &&
    !streaming &&
    !overLimit &&
    !uploading;

  const packageText = async (text: string) => {
    if (attachments.length >= env.maxUploadCount) {
      toast.error(copy.uploadMax(env.maxUploadCount));
      setValue(text);
      return;
    }

    const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
    const fileName = `ai-message-${timestamp}.txt`;
    const file = new File([text], fileName, { type: "text/plain" });
    if (file.size > env.maxUploadBytes) {
      toast.error(copy.textTooLarge);
      setValue(text);
      return;
    }

    const id = `att-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
    const pending: Attachment = {
      id,
      name: file.name,
      size: file.size,
      type: file.type,
      sourceFile: file,
      status: "uploading",
    };
    setAttachments((prev) => [...prev, pending]);
    setValue("");

    try {
      const url = await fileApi.upload(file, uploadPrefix);
      setAttachments((prev) =>
        prev.map((item) => (item.id === id ? { ...item, url, status: "done" } : item)),
      );
    } catch {
      toast.error(copy.uploadFailed(file.name));
      setAttachments((prev) => prev.filter((item) => item.id !== id));
      setValue((current) => (current ? `${text}\n${current}` : text));
    }
  };

  const handleValueChange = (nextValue: string) => {
    if (nextValue.length <= env.autoTextAttachmentLength) {
      setValue(nextValue);
      return;
    }
    void packageText(nextValue);
  };

  const handleSubmit = async () => {
    if (!canSend) return;
    const text = value.trim() || copy.attachmentPrompt;
    onSubmit(text, {
      attachments: attachments.filter((a) => a.status === "done"),
      redesignOption: designMode,
      pubmedYear,
      pubmedArticleType: pubmedType,
    });
    setValue("");
    setAttachments([]);
  };

  const handleFiles = async (files: FileList | null) => {
    if (!files) return;

    const incoming = Array.from(files);
    const remain = env.maxUploadCount - attachments.length;
    if (remain <= 0) {
      toast.error(copy.uploadMax(env.maxUploadCount));
      return;
    }
    if (incoming.length > remain) {
      toast.error(copy.uploadRemain(env.maxUploadCount, remain));
    }

    for (const file of incoming.slice(0, remain)) {
      if (file.size > env.maxUploadBytes) {
        toast.error(copy.fileTooLarge(file.name));
        continue;
      }
      const id = `att-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
      const pending: Attachment = {
        id,
        name: file.name,
        size: file.size,
        type: file.type,
        sourceFile: file,
        status: "uploading",
      };
      setAttachments((prev) => [...prev, pending]);
      try {
        const url = await fileApi.upload(file, uploadPrefix);
        setAttachments((prev) =>
          prev.map((a) => (a.id === id ? { ...a, url, status: "done" } : a)),
        );
      } catch {
        toast.error(copy.uploadFailed(file.name));
        setAttachments((prev) => prev.map((a) => (a.id === id ? { ...a, status: "error" } : a)));
      }
    }
  };

  return (
    <div className={cn(CHAT_COLUMN_CLASS, "pb-4")}>
      <div className="rounded-2xl border bg-card p-3 shadow-sm">
        {attachments.length > 0 && (
          <div className="mb-2 flex flex-wrap gap-1.5">
            {attachments.map((a) => (
              <FileChip
                key={a.id}
                attachment={a}
                onRemove={() =>
                  setAttachments((prev) => prev.filter((x) => x.id !== a.id))
                }
              />
            ))}
          </div>
        )}

        <Textarea
          value={value}
          onChange={(e) => handleValueChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              void handleSubmit();
            }
          }}
          placeholder={resolvedPlaceholder}
          className="max-h-40 min-h-12 resize-none border-0 shadow-none focus-visible:ring-0"
        />

        <div className="flex items-center justify-between gap-2 pt-1">
          <div className="flex min-w-0 flex-wrap items-center gap-2">
            {modeSelect && (
              <ModeSelect
                modes={modeSelect.modes}
                value={modeSelect.value}
                onChange={modeSelect.onChange}
                disabled={streaming}
              />
            )}
            {designMode && onDesignModeChange && (
              <div className="flex items-center rounded-full border border-[#E5E7EB] bg-white p-0.5">
                {DESIGN_MODES.map((opt) => (
                  <button
                    key={opt}
                    type="button"
                    disabled={streaming}
                    onClick={() => onDesignModeChange(opt)}
                    className={cn(
                      "rounded-full px-2.5 py-1 text-xs transition-colors disabled:opacity-50",
                      designMode === opt
                        ? "bg-[#EDF3FE] font-medium text-[#4D51F4]"
                        : "text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {DESIGN_MODE_LABELS[locale][opt]}
                  </button>
                ))}
              </div>
            )}
            {pubmedYear && onPubmedYearChange && (
              <Select
                value={pubmedYear}
                onValueChange={(v) => onPubmedYearChange((v as PubmedYear) || "unlimited")}
                disabled={streaming}
              >
                <SelectTrigger size="sm" className="h-8 rounded-full">
                  <SelectValue>
                    {yearLabels[pubmedYear] ?? copy.pubmedYearUnlimited}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {PUBMED_YEAR_OPTIONS.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {yearLabels[o.value]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            {pubmedType && onPubmedTypeChange && (
              <Select
                value={pubmedType}
                onValueChange={(v) =>
                  onPubmedTypeChange((v as PubmedArticleType) || "unlimited")
                }
                disabled={streaming}
              >
                <SelectTrigger size="sm" className="h-8 rounded-full">
                  <SelectValue>
                    {typeLabels[pubmedType] ?? copy.pubmedTypeUnlimited}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {PUBMED_TYPE_OPTIONS.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {typeLabels[o.value]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            {overLimit && (
              <span className="text-xs text-destructive">
                {copy.textTooLarge}
              </span>
            )}
          </div>

          <div className="flex items-center gap-1">
            {allowAttachments && (
              <>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={acceptTypes}
                  multiple
                  className="hidden"
                  onChange={(e) => {
                    handleFiles(e.target.files);
                    e.target.value = "";
                  }}
                />
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={copy.uploadFile}
                  title={copy.uploadHint(env.maxUploadCount)}
                  onClick={() => fileInputRef.current?.click()}
                  disabled={streaming || attachments.length >= env.maxUploadCount}
                >
                  <Paperclip className="size-4" />
                </Button>
              </>
            )}
            {streaming ? (
              <Button
                size="icon"
                className="bg-[#EF4444] text-white hover:bg-[#DC2626]"
                onClick={onStop}
                aria-label={copy.stop}
              >
                <span className="block size-2.5 rounded-[2px] bg-white" />
              </Button>
            ) : (
              <Button
                size="icon"
                className="bg-brand text-brand-foreground hover:bg-brand/90"
                disabled={!canSend}
                onClick={() => void handleSubmit()}
                aria-label={copy.send}
              >
                <ArrowUp className="size-4" />
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
