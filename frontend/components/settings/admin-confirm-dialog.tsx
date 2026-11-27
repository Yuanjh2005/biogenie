"use client";

import { createContext, ReactNode, useCallback, useContext, useRef, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCountdown } from "@/hooks/useCountdown";
import { adminApi } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/errors";
import { useLocaleStore } from "@/stores/locale";
import { ADMIN_SETTINGS_COPY } from "@/lib/i18n/admin-settings";

const AdminConfirmContext = createContext<(() => Promise<string>) | null>(null);

export class AdminConfirmCancelledError extends Error {}

export function AdminConfirmProvider({ children }: { children: ReactNode }) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = ADMIN_SETTINGS_COPY[locale];
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState("");
  const [phoneHint, setPhoneHint] = useState("");
  const [sending, setSending] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const pending = useRef<{ resolve: (token: string) => void; reject: (reason: Error) => void } | null>(null);
  const { remaining, active, start } = useCountdown(60);

  const requestConfirm = useCallback(() => new Promise<string>((resolve, reject) => {
    pending.current?.reject(new AdminConfirmCancelledError("二次确认已被新操作替代"));
    pending.current = { resolve, reject };
    setCode("");
    setPhoneHint("");
    setOpen(true);
  }), []);

  const close = () => {
    pending.current?.reject(new AdminConfirmCancelledError("已取消二次确认"));
    pending.current = null;
    setOpen(false);
    setCode("");
  };

  const send = async () => {
    if (active || sending) return;
    setSending(true);
    try {
      setPhoneHint(await adminApi.sendSecondFactorCode());
      start();
      toast.success(copy.codeSent);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : copy.sendCodeFailed);
    } finally { setSending(false); }
  };

  const submit = async () => {
    if (!/^\d{4,8}$/.test(code) || submitting) return;
    setSubmitting(true);
    try {
      const result = await adminApi.verifySecondFactor(code);
      const resolver = pending.current;
      pending.current = null;
      setOpen(false);
      setCode("");
      resolver?.resolve(result.confirmToken);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : copy.verifyFailed);
    } finally { setSubmitting(false); }
  };

  return <AdminConfirmContext.Provider value={requestConfirm}>
    {children}
    <Dialog open={open} onOpenChange={(next) => { if (!next) close(); }}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader><DialogTitle>{copy.confirmTitle}</DialogTitle></DialogHeader>
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">{copy.confirmDescription}</p>
          {phoneHint && <p className="rounded-md bg-muted px-3 py-2 text-xs">{phoneHint}</p>}
          <div className="space-y-2">
            <Label htmlFor="admin-confirm-code">{copy.verificationCode}</Label>
            <div className="flex gap-2">
              <Input id="admin-confirm-code" inputMode="numeric" maxLength={8} value={code}
                placeholder={copy.codePlaceholder} autoFocus
                onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
                onKeyDown={(event) => { if (event.key === "Enter") void submit(); }} />
              <Button type="button" variant="outline" className="shrink-0" disabled={active || sending} onClick={send}>
                {active ? `${remaining}s` : sending ? copy.sending : copy.sendCode}
              </Button>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={close}>{copy.cancel}</Button>
          <Button disabled={!/^\d{4,8}$/.test(code) || submitting} onClick={submit}>
            {submitting ? copy.verifying : copy.confirmContinue}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </AdminConfirmContext.Provider>;
}

export function useAdminConfirm() {
  const value = useContext(AdminConfirmContext);
  if (!value) throw new Error("useAdminConfirm 必须在 AdminConfirmProvider 内使用");
  return value;
}
