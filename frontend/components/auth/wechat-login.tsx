"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { useAuthStore } from "@/stores/auth";
import { LOGIN_COPY } from "@/lib/i18n/login";
import { useLocaleStore } from "@/stores/locale";

const STATE_KEY = "bg_wechat_state";

export function WechatLogin() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const setSession = useAuthStore((s) => s.setSession);
  const locale = useLocaleStore((s) => s.locale);
  const copy = LOGIN_COPY[locale];
  const [open, setOpen] = useState(false);
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [comingSoon, setComingSoon] = useState(false);

  // 处理微信回调：URL 带 code + state
  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");
    if (!code || !state) return;
    const savedState = sessionStorage.getItem(STATE_KEY);
    if (savedState && savedState !== state) {
      toast.error(LOGIN_COPY[useLocaleStore.getState().locale].wechatStateFailed);
      return;
    }
    authApi
      .loginByWechat(code, state)
      .then((result) => {
        setSession(result);
        toast.success(LOGIN_COPY[useLocaleStore.getState().locale].loginSuccess);
        router.replace("/home");
      })
      .catch((e) => {
        toast.error(
          e instanceof ApiError
            ? e.message
            : LOGIN_COPY[useLocaleStore.getState().locale].wechatLoginFailed,
        );
      });
  }, [searchParams, router, setSession]);

  // 仅在用户点击入口后拉取二维码配置。
  const loadQrCode = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      const cfg = await authApi.getWechatQrConfig();
      if (!cfg.appId || cfg.appId === "your-wechat-app-id") {
        setComingSoon(true);
        return;
      }
      sessionStorage.setItem(STATE_KEY, cfg.state);
      setQrUrl(cfg.qrConnectUrl);
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : LOGIN_COPY[useLocaleStore.getState().locale].qrFailed,
      );
    } finally {
      setLoading(false);
    }
  };

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (nextOpen && !qrUrl) void loadQrCode();
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <div className="flex items-center gap-3 py-1 before:h-px before:flex-1 before:bg-border after:h-px after:flex-1 after:bg-border">
        <DialogTrigger className="group flex shrink-0 items-center gap-2 rounded-full px-3 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-[#07c160]/10 hover:text-[#07c160] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#07c160]/40">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/images/login/wechat-icon.svg" alt="" className="size-6 rounded-full" />
          <span>{copy.wechat}</span>
        </DialogTrigger>
      </div>

      <DialogContent className="w-[360px] max-w-[calc(100%-2rem)] overflow-hidden p-6 sm:max-w-[360px]">
        <DialogHeader className="items-center text-center">
          <DialogTitle className="text-lg">{copy.wechat}</DialogTitle>
          <DialogDescription>{copy.wechatScanTip}</DialogDescription>
        </DialogHeader>
        <div className="mx-auto mt-1 flex h-[360px] w-[300px] max-w-full items-center justify-center overflow-hidden rounded-2xl border border-border/70 bg-white shadow-inner">
          {loading ? (
            <Skeleton className="size-full rounded-2xl" />
          ) : comingSoon ? (
            <div className="flex size-full flex-col items-center justify-center gap-4 bg-gradient-to-b from-[#07c160]/5 to-white px-8 text-center">
              <div className="flex size-16 items-center justify-center rounded-2xl bg-[#07c160]/10 shadow-sm">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src="/images/login/wechat-icon.svg" alt="" className="size-9 rounded-full" />
              </div>
              <div>
                <p className="text-base font-semibold text-foreground">{copy.featureInDevelopment}</p>
                <p className="mt-1 text-sm text-muted-foreground">{copy.stayTuned}</p>
              </div>
            </div>
          ) : error ? (
            <div className="flex size-full flex-col items-center justify-center gap-3 bg-muted/30 px-8 text-center text-sm text-muted-foreground">
              <span>{error}</span>
              <button
                type="button"
                className="text-brand hover:underline"
                onClick={() => {
                  setQrUrl(null);
                  void loadQrCode();
                }}
              >
                {copy.retry}
              </button>
            </div>
          ) : (
            <iframe
              title={copy.wechat}
              src={qrUrl ?? undefined}
              className="size-full border-0"
              sandbox="allow-scripts allow-same-origin allow-popups allow-forms"
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
