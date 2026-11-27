"use client";

import { type ReactNode, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { isValidCode, isValidPhone } from "@/lib/validators";
import { LOGIN_COPY } from "@/lib/i18n/login";
import { useCountdown } from "@/hooks/useCountdown";
import { useAuthStore } from "@/stores/auth";
import { useLocaleStore } from "@/stores/locale";

export function PhoneLoginForm({ afterSubmit }: { afterSubmit?: ReactNode }) {
  const router = useRouter();
  const setSession = useAuthStore((s) => s.setSession);
  const locale = useLocaleStore((s) => s.locale);
  const copy = LOGIN_COPY[locale];
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [sending, setSending] = useState(false);
  const [loggingIn, setLoggingIn] = useState(false);
  const { remaining, active, start } = useCountdown(60);

  const phoneOk = isValidPhone(phone);
  const canLogin = phoneOk && isValidCode(code) && !loggingIn;

  const handleSendCode = async () => {
    if (!phoneOk || active || sending) return;
    setSending(true);
    try {
      await authApi.sendSmsCode(phone.trim());
      start();
      toast.success(copy.codeSent);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : copy.sendCodeFailed);
    } finally {
      setSending(false);
    }
  };

  const handleLogin = async () => {
    if (!canLogin) return;
    setLoggingIn(true);
    try {
      const result = await authApi.login(phone.trim(), code.trim());
      setSession(result);
      toast.success(copy.loginSuccess);
      router.replace("/home");
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : copy.loginFailed);
    } finally {
      setLoggingIn(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="phone">{copy.phone}</Label>
        <div className="flex gap-2">
          <Input
            id="phone"
            inputMode="numeric"
            maxLength={11}
            placeholder={copy.phonePlaceholder}
            value={phone}
            onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
            autoComplete="tel"
            className="h-11 rounded-xl px-3.5"
          />
          <Button
            type="button"
            variant="outline"
            className="h-11 shrink-0 rounded-xl px-3"
            disabled={!phoneOk || active || sending}
            onClick={handleSendCode}
          >
            {active ? `${remaining}s` : sending ? copy.sending : copy.sendCode}
          </Button>
        </div>
      </div>
      <div className="space-y-2">
        <Label htmlFor="code">{copy.code}</Label>
        <Input
          id="code"
          inputMode="numeric"
          maxLength={6}
          placeholder={copy.codePlaceholder}
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
          onKeyDown={(e) => e.key === "Enter" && handleLogin()}
          className="h-11 rounded-xl px-3.5"
        />
      </div>
      <Button
        type="button"
        className="h-11 w-full rounded-xl bg-brand text-brand-foreground hover:bg-brand/90"
        disabled={!canLogin}
        onClick={handleLogin}
      >
        {loggingIn ? copy.loggingIn : copy.login}
      </Button>
      {afterSubmit}
    </div>
  );
}
