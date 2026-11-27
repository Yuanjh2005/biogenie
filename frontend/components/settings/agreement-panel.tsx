"use client";

import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import type { AgreementType } from "@/components/settings/agreement-dialog";
import { SETTINGS_COPY } from "@/lib/i18n/settings";
import { useLocaleStore } from "@/stores/locale";

interface AgreementPanelProps {
  onOpen: (type: AgreementType) => void;
}

export function AgreementPanel({ onOpen }: AgreementPanelProps) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = SETTINGS_COPY[locale];

  return (
    <div>
      <div className="flex items-center justify-between py-3">
        <span className="text-sm">{copy.userAgreement}</span>
        <Button variant="outline" size="sm" onClick={() => onOpen("user")}>
          {copy.view}
        </Button>
      </div>
      <Separator />
      <div className="flex items-center justify-between py-3">
        <span className="text-sm">{copy.privacyPolicy}</span>
        <Button variant="outline" size="sm" onClick={() => onOpen("privacy")}>
          {copy.view}
        </Button>
      </div>
    </div>
  );
}
