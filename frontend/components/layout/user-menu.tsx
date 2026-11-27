"use client";

import { useState } from "react";
import { Settings } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { SettingsDialog } from "@/components/settings/settings-dialog";
import { useAuthStore } from "@/stores/auth";
import { maskPhone } from "@/lib/validators";
import { SHELL_COPY } from "@/lib/i18n/shell";
import { localizeUserNickname } from "@/lib/i18n/user";
import { useLocaleStore } from "@/stores/locale";

export function UserMenu() {
  const user = useAuthStore((s) => s.user);
  const [open, setOpen] = useState(false);
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHELL_COPY[locale];
  const nickname = localizeUserNickname(user?.nickname, locale);

  return (
    <div className="flex shrink-0 items-center gap-2 border-t p-3">
      <Avatar className="size-9">
        <AvatarImage src={user?.avatar || undefined} alt={nickname} />
        <AvatarFallback>{nickname[0] ?? "U"}</AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium">{nickname || copy.userFallback}</div>
        <div className="truncate text-xs text-muted-foreground">
          {maskPhone(user?.phone)}
        </div>
      </div>
      <Button
        size="icon"
        variant="ghost"
        aria-label={copy.settings}
        onClick={() => setOpen(true)}
      >
        <Settings className="size-4" />
      </Button>
      <SettingsDialog open={open} onOpenChange={setOpen} />
    </div>
  );
}
