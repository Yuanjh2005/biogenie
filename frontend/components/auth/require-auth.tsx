"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/auth";
import { useLocaleStore } from "@/stores/locale";
import { SHELL_COPY } from "@/lib/i18n/shell";

/** 业务页守卫：未登录跳转 /login；等待 hydrate 完成再判断，避免刷新闪烁。 */
export function RequireAuth({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const hydrated = useAuthStore((s) => s.hydrated);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHELL_COPY[locale];

  useEffect(() => {
    if (hydrated && !isAuthenticated) {
      router.replace("/login");
    }
  }, [hydrated, isAuthenticated, router]);

  if (!hydrated || !isAuthenticated) {
    return (
      <div className="flex h-screen items-center justify-center text-muted-foreground">
        {copy.loading}
      </div>
    );
  }

  return <>{children}</>;
}
