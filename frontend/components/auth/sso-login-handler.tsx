"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { useAuthStore } from "@/stores/auth";
import { useLocaleStore } from "@/stores/locale";
import { SHELL_COPY } from "@/lib/i18n/shell";
import { LOGIN_COPY } from "@/lib/i18n/login";
import { MODULES } from "@/lib/config/agents";
import { saveSsoLaunch } from "@/lib/auth/sso-launch";
import type { LoginResultDTO } from "@/types/api";

/** 同一 ticket 复用请求，避免 React Strict Mode 双调用把一次性票据消费两次。 */
const inflightLogins = new Map<string, Promise<LoginResultDTO>>();

function loginBySsoOnce(ticket: string): Promise<LoginResultDTO> {
  const existing = inflightLogins.get(ticket);
  if (existing) return existing;
  const request = authApi.loginBySso(ticket);
  inflightLogins.set(ticket, request);
  return request;
}

export function SsoLoading() {
  const copy = SHELL_COPY.en;
  return (
    <div className="flex h-screen items-center justify-center text-muted-foreground">
      {copy.loading}
    </div>
  );
}

/** 读取 URL ticket，静默登录后进入指定模块（无启动信息则进入首页）。 */
export function SsoLoginHandler() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const setSession = useAuthStore((s) => s.setSession);
  const started = useRef(false);
  // Strict Mode 会模拟一次 effect 清理和重建；用共享 ref 反映最终挂载状态。
  const mounted = useRef(false);

  useEffect(() => {
    mounted.current = true;
    if (started.current) {
      return () => {
        mounted.current = false;
      };
    }
    started.current = true;

    const ticket = searchParams.get("ticket")?.trim() ?? "";
    if (!ticket) {
      router.replace("/login");
      return;
    }

    loginBySsoOnce(ticket)
      .then((result) => {
        if (!mounted.current) return;
        // 每次外部免登成功都以英文进入；在展示业务页面前保存语言。
        useLocaleStore.getState().setLocale("en");
        setSession(result);
        const targetModule = result.targetModule;
        const question = result.question?.trim();
        const attachments = result.attachments?.map((attachment, index) => ({
          id: `sso-${index}-${attachment.name ?? "attachment"}`,
          name: attachment.name ?? "attachment",
          size: attachment.size ?? 0,
          type: attachment.contentType ?? "application/octet-stream",
          url: attachment.url,
          status: "done" as const,
        }));
        if (targetModule && (question || attachments?.length) && targetModule in MODULES) {
          saveSsoLaunch({ module: targetModule, question, attachments });
          router.replace(MODULES[targetModule].route);
          return;
        }
        router.replace("/home");
      })
      .catch((e: unknown) => {
        if (!mounted.current) return;
        toast.error(e instanceof ApiError ? e.message : LOGIN_COPY[useLocaleStore.getState().locale].loginFailed);
        router.replace("/login");
      });

    return () => {
      mounted.current = false;
    };
  }, [searchParams, router, setSession]);

  return <SsoLoading />;
}
