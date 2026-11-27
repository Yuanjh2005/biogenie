"use client";

import { useEffect, useState } from "react";
import { Menu } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";
import { SidebarContent } from "@/components/layout/sidebar";
import { SHELL_COPY } from "@/lib/i18n/shell";
import { useLocaleStore } from "@/stores/locale";

export function AppShell({ children }: { children: React.ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHELL_COPY[locale];

  useEffect(() => {
    const html = document.documentElement;
    const { overflow: htmlOverflow } = html.style;
    const { overflow: bodyOverflow } = document.body.style;
    html.style.overflow = "hidden";
    document.body.style.overflow = "hidden";
    window.scrollTo(0, 0);
    return () => {
      html.style.overflow = htmlOverflow;
      document.body.style.overflow = bodyOverflow;
    };
  }, []);

  return (
    <div className="flex h-screen overflow-hidden">
      {/* 桌面固定侧边栏 */}
      <aside className="hidden w-64 shrink-0 border-r md:block">
        <SidebarContent />
      </aside>

      {/* 移动端抽屉 */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="w-64 p-0">
          <SheetTitle className="sr-only">{copy.navMenu}</SheetTitle>
          <SidebarContent onNavigate={() => setMobileOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* 移动端顶栏 */}
        <header className="flex items-center gap-2 border-b p-2 md:hidden">
          <Button
            size="icon"
            variant="ghost"
            aria-label={copy.openMenu}
            onClick={() => setMobileOpen(true)}
          >
            <Menu className="size-5" />
          </Button>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/images/login/logo.svg" alt="BioGenie" className="h-6 w-auto" />
        </header>

        <main className="min-h-0 flex-1 overflow-hidden">{children}</main>
      </div>
    </div>
  );
}
