"use client";

import { useMemo, useState } from "react";
import { BarChart3, FileClock, FileText, MessageSquareText, User, Users } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/auth";
import {
  AgreementDialog,
  useAgreementDialog,
} from "@/components/settings/agreement-dialog";
import { AccountPanel } from "@/components/settings/account-panel";
import { AgreementPanel } from "@/components/settings/agreement-panel";
import { UserManagementPanel } from "@/components/settings/user-management-panel";
import { OperationsPanel } from "@/components/settings/operations-panel";
import { LogsPanel } from "@/components/settings/logs-panel";
import { ConversationManagementPanel } from "@/components/settings/conversation-management-panel";
import { AdminConfirmProvider } from "@/components/settings/admin-confirm-dialog";
import { SETTINGS_COPY } from "@/lib/i18n/settings";
import { useLocaleStore } from "@/stores/locale";
import { LanguageSwitcher } from "@/components/auth/language-switcher";
import { ADMIN_SETTINGS_COPY } from "@/lib/i18n/admin-settings";

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

type Section = "account" | "agreement" | "users" | "conversations" | "operations" | "logs";

export function SettingsDialog({ open, onOpenChange }: SettingsDialogProps) {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === "ADMIN" || user?.role === "SUPER_ADMIN";
  const isSuperAdmin = user?.role === "SUPER_ADMIN";
  const agreement = useAgreementDialog();
  const [section, setSection] = useState<Section>("account");
  const locale = useLocaleStore((s) => s.locale);
  const copy = SETTINGS_COPY[locale];
  const adminCopy = ADMIN_SETTINGS_COPY[locale];

  const navItems = useMemo(
    () => [
      { id: "account" as const, label: copy.account, icon: User },
      ...(isAdmin ? [{ id: "users" as const, label: copy.users, icon: Users }] : []),
      ...(isAdmin ? [{ id: "conversations" as const, label: adminCopy.conversations, icon: MessageSquareText }] : []),
      ...(isSuperAdmin ? [{ id: "operations" as const, label: adminCopy.operations, icon: BarChart3 }] : []),
      ...(isSuperAdmin ? [{ id: "logs" as const, label: adminCopy.logs, icon: FileClock }] : []),
      { id: "agreement" as const, label: copy.agreement, icon: FileText },
    ],
    [isAdmin, isSuperAdmin, copy.account, copy.agreement, copy.users, adminCopy.conversations, adminCopy.operations, adminCopy.logs],
  );

  return (
    <AdminConfirmProvider>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="h-[min(760px,calc(100vh-2rem))] w-[min(960px,calc(100vw-2rem))] grid-rows-[auto_minmax(0,1fr)] overflow-hidden sm:max-w-4xl">
          <DialogHeader>
            <div className="flex items-center justify-between gap-3 pr-8">
              <DialogTitle>{copy.title}</DialogTitle>
              <LanguageSwitcher />
            </div>
          </DialogHeader>

          <div className="flex min-h-0 gap-4 overflow-hidden">
            <nav className="w-36 shrink-0 space-y-1 overflow-y-auto border-r pr-3">
              {navItems.map((item) => {
                const Icon = item.icon;
                const active = section === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setSection(item.id)}
                    className={cn(
                      "flex w-full items-center gap-2 rounded-md px-2.5 py-2 text-left text-sm hover:bg-accent",
                      active && "bg-accent font-medium",
                    )}
                  >
                    <Icon className="size-4" />
                    {item.label}
                  </button>
                );
              })}
            </nav>
            <div className="min-h-0 min-w-0 flex-1 overflow-x-hidden overflow-y-auto pr-1">
              {section === "account" && <AccountPanel />}
              {section === "agreement" && (
                <AgreementPanel onOpen={agreement.open} />
              )}
              {section === "users" && isAdmin && <UserManagementPanel />}
              {section === "conversations" && isAdmin && <ConversationManagementPanel />}
              {section === "operations" && isSuperAdmin && <OperationsPanel />}
              {section === "logs" && isSuperAdmin && <LogsPanel />}
            </div>
          </div>
        </DialogContent>
      </Dialog>
      <AgreementDialog type={agreement.type} onOpenChange={agreement.onOpenChange} />
    </AdminConfirmProvider>
  );
}
