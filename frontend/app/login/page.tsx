"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { PhoneLoginForm } from "@/components/auth/phone-login-form";
import { PasswordLoginForm } from "@/components/auth/password-login-form";
import { WechatLogin } from "@/components/auth/wechat-login";
import { LanguageSwitcher } from "@/components/auth/language-switcher";
import {
  AgreementDialog,
  useAgreementDialog,
} from "@/components/settings/agreement-dialog";
import { LOGIN_COPY } from "@/lib/i18n/login";
import { useAuthStore } from "@/stores/auth";
import { useLocaleStore } from "@/stores/locale";

export default function LoginPage() {
  const router = useRouter();
  const hydrated = useAuthStore((s) => s.hydrated);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const locale = useLocaleStore((s) => s.locale);
  const agreement = useAgreementDialog();
  const copy = LOGIN_COPY[locale];
  const [loginMode, setLoginMode] = useState<"password" | "sms">("password");
  const wechatEntry = (
    <Suspense
      fallback={
        <div className="h-10 animate-pulse rounded-full bg-muted/50" />
      }
    >
      <WechatLogin />
    </Suspense>
  );

  useEffect(() => {
    if (hydrated && isAuthenticated) router.replace("/home");
  }, [hydrated, isAuthenticated, router]);

  return (
    <div className="flex min-h-screen bg-white">
      <div className="relative hidden w-[34%] max-w-[460px] shrink-0 overflow-hidden lg:block">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/images/login/login-bg.svg"
          alt=""
          className="absolute inset-0 size-full object-cover"
        />
        <div className="relative z-10 flex h-full flex-col items-start px-10 pt-10 text-left">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/images/login/logo.svg"
            alt="BioGenie"
            className="h-10 w-auto"
          />
          <div className="mt-16 w-full">
            <h1 className="text-2xl font-bold leading-snug text-slate-900">
              {copy.brandTitle}
            </h1>
            <p className="mt-3 text-base leading-relaxed text-slate-500">
              {copy.brandSubtitle}
            </p>
          </div>
        </div>
      </div>

      <div className="relative flex flex-1 flex-col">
        <div className="flex justify-end px-6 pt-6">
          <LanguageSwitcher />
        </div>

        <div className="flex flex-1 items-center justify-center px-6 pb-16">
          <div className="w-full max-w-2xl">
            <div className="flex justify-center">
              <div className="w-full max-w-sm">
                <div className="mb-8 text-center">
                  <h2 className="text-2xl font-bold">{copy.heading}</h2>
                  <p className="mt-2 text-sm text-muted-foreground">{copy.welcome}</p>
                </div>
                <div className="mb-6 flex justify-center gap-8 border-b">
                  {(["password", "sms"] as const).map((mode) => (
                    <button
                      key={mode}
                      type="button"
                      onClick={() => setLoginMode(mode)}
                      className={`relative pb-3 text-base font-semibold transition-colors ${
                        loginMode === mode ? "text-brand" : "text-foreground"
                      }`}
                    >
                      {mode === "password" ? copy.passwordLogin : copy.smsLogin}
                      {loginMode === mode && <span className="absolute inset-x-0 bottom-0 h-0.5 rounded-full bg-brand" />}
                    </button>
                  ))}
                </div>
                {loginMode === "password" ? (
                  <PasswordLoginForm afterSubmit={wechatEntry} />
                ) : (
                  <PhoneLoginForm afterSubmit={wechatEntry} />
                )}
                <p className="mt-6 text-center text-xs leading-relaxed text-muted-foreground">
                  {copy.agreePrefix}
                  <button
                    type="button"
                    className="mx-1 text-black underline"
                    onClick={() => agreement.open("user")}
                  >
                    {copy.terms}
                  </button>
                  {copy.and}
                  <button
                    type="button"
                    className="mx-1 text-black underline"
                    onClick={() => agreement.open("privacy")}
                  >
                    {copy.privacy}
                  </button>
                  {copy.agreeEnd}
                  <br />
                  {copy.autoRegister}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <AgreementDialog type={agreement.type} onOpenChange={agreement.onOpenChange} />
    </div>
  );
}
