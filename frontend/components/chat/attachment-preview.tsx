"use client";

import { useEffect, useMemo, useState, type ReactElement } from "react";
import { ExternalLink, FileText, Loader2 } from "lucide-react";
import { Dialog, DialogContent, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import type { Attachment } from "@/types/chat";

type PreviewKind = "text" | "image" | "pdf" | "unsupported";

function previewKind(attachment: Attachment): PreviewKind {
  const type = attachment.type.toLowerCase();
  const extension = attachment.name.split(".").pop()?.toLowerCase();
  if (type.startsWith("image/") || ["png", "jpg", "jpeg", "gif", "webp", "svg"].includes(extension || "")) return "image";
  if (type === "application/pdf" || extension === "pdf") return "pdf";
  if (type.startsWith("text/") || ["txt", "md", "csv", "json", "xml", "log"].includes(extension || "")) return "text";
  return "unsupported";
}

export function AttachmentPreview({ attachment, disabled = false, children }: {
  attachment: Attachment;
  disabled?: boolean;
  children: ReactElement;
}) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const kind = previewKind(attachment);
  const localUrl = useMemo(
    () => (attachment.sourceFile ? URL.createObjectURL(attachment.sourceFile) : undefined),
    [attachment.sourceFile],
  );
  const source = localUrl || attachment.url;

  useEffect(() => () => {
    if (localUrl) URL.revokeObjectURL(localUrl);
  }, [localUrl]);

  const handleOpenChange = (next: boolean) => {
    if (disabled) return;
    setOpen(next);
    if (!next || kind !== "text" || text || loading) return;

    setLoading(true);
    setFailed(false);
    const read = attachment.sourceFile
      ? attachment.sourceFile.text()
      : fetch(attachment.url as string).then((response) => {
          if (!response.ok) throw new Error(`Preview request failed: ${response.status}`);
          return response.text();
        });
    void read
      .then(setText)
      .catch(() => setFailed(true))
      .finally(() => setLoading(false));
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger disabled={disabled} render={children} />
      <DialogContent className="top-0 right-0 left-auto flex h-dvh w-full max-w-none translate-x-0 translate-y-0 flex-col gap-0 rounded-none p-0 sm:w-[min(760px,58vw)] sm:max-w-none">
        <header className="flex h-16 shrink-0 items-center border-b px-6 pr-14">
          <DialogTitle className="truncate text-base font-semibold">{attachment.name}</DialogTitle>
        </header>
        <div className="min-h-0 flex-1 overflow-auto bg-background">
          {kind === "text" && (
            <div className="mx-auto max-w-3xl whitespace-pre-wrap break-words px-6 py-5 text-sm leading-7 text-foreground">
              {loading ? (
                <span className="flex items-center gap-2 text-muted-foreground"><Loader2 className="size-4 animate-spin" />{copy.previewLoading}</span>
              ) : failed ? copy.previewFailed : text}
            </div>
          )}
          {kind === "image" && source && (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={source} alt={attachment.name} className="mx-auto h-full max-h-full w-full object-contain p-6" />
          )}
          {kind === "pdf" && source && (
            <iframe src={source} title={attachment.name} className="h-full min-h-[calc(100vh-4rem)] w-full border-0" />
          )}
          {kind === "unsupported" && (
            <div className="flex h-full min-h-80 flex-col items-center justify-center gap-4 px-6 text-center">
              <span className="flex size-14 items-center justify-center rounded-2xl bg-blue-500 text-white"><FileText className="size-7" /></span>
              <div>
                <p className="font-medium">{attachment.name}</p>
                <p className="mt-1 text-sm text-muted-foreground">{copy.previewUnsupported}</p>
              </div>
              {source && (
                <Button variant="outline" render={<a href={source} target="_blank" rel="noreferrer" />}>
                  {copy.openOriginal}<ExternalLink className="size-4" />
                </Button>
              )}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
