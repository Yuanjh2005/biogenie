"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/sonner";
import { useAuthStore } from "@/stores/auth";
import { useChatStore } from "@/stores/chat";
import { useLocaleStore } from "@/stores/locale";
import { registerUnauthorizedHandler } from "@/lib/api/errors";
import { userApi } from "@/lib/api/user";
import { getToken } from "@/lib/auth/token";

export function Providers({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const hydrate = useAuthStore((s) => s.hydrate);
  const hydrateLocale = useLocaleStore((s) => s.hydrate);
  const reset = useAuthStore((s) => s.reset);
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 },
        },
      }),
  );
  const registered = useRef(false);

  useEffect(() => {
    hydrate();
    hydrateLocale();
    if (getToken()) {
      const storedAvatar = useAuthStore.getState().user?.avatar;
      userApi.profile().then((profile) => {
        const current = useAuthStore.getState();
        if (current.isAuthenticated && current.user?.id === profile.id && current.user.avatar === storedAvatar) {
          current.setUser(profile);
        }
      }).catch(() => {
        // 离线时保留本地登录资料；下次加载再同步头像链接。
      });
    }
    if (!registered.current) {
      registered.current = true;
      registerUnauthorizedHandler(() => {
        reset();
        useChatStore.getState().reset();
        router.replace("/login");
      });
    }
  }, [hydrate, hydrateLocale, reset, router]);

  useEffect(() => {
    return useAuthStore.subscribe((state, prev) => {
      if (prev.isAuthenticated && !state.isAuthenticated) {
        useChatStore.getState().reset();
      }
    });
  }, []);

  return (
    <QueryClientProvider client={client}>
      {children}
      <Toaster richColors position="top-center" />
    </QueryClientProvider>
  );
}
