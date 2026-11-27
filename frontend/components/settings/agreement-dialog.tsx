"use client";

import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useState } from "react";
import {
  AGREEMENT_CONTENT,
  AGREEMENT_TITLES,
  type AgreementType,
} from "@/lib/legal/agreements";
import { SETTINGS_COPY } from "@/lib/i18n/settings";
import { useLocaleStore } from "@/stores/locale";

export type { AgreementType };

interface AgreementDialogProps {
  type: AgreementType | null;
  onOpenChange: (open: boolean) => void;
}

export function AgreementDialog({ type, onOpenChange }: AgreementDialogProps) {
  const locale = useLocaleStore((s) => s.locale);

  return (
    <Dialog open={type !== null} onOpenChange={onOpenChange}>
      <DialogContent className="flex h-[min(85vh,720px)] flex-col gap-0 overflow-hidden p-0 sm:max-w-2xl">
        <DialogHeader className="border-b px-6 py-4 pr-12">
          <DialogTitle className="text-lg">
            {type ? AGREEMENT_TITLES[locale][type] : ""}
          </DialogTitle>
        </DialogHeader>
        <ScrollArea className="min-h-0 flex-1 px-6">
          <p className="whitespace-pre-wrap py-4 text-sm leading-relaxed text-foreground/80">
            {type ? AGREEMENT_CONTENT[locale][type] : ""}
          </p>
        </ScrollArea>
        <DialogFooter className="m-0 rounded-none" showCloseButton={false}>
          <Button type="button" onClick={() => onOpenChange(false)}>
            {SETTINGS_COPY[locale].close}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** 便捷 hook：管理协议弹窗打开状态 */
export function useAgreementDialog() {
  const [type, setType] = useState<AgreementType | null>(null);
  return {
    type,
    open: (t: AgreementType) => setType(t),
    onOpenChange: (open: boolean) => {
      if (!open) setType(null);
    },
  };
}
