"use client";

import { MODULE_META } from "@/components/layout/module-meta";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import type { ModuleKey } from "@/types/chat";

export function Welcome({ module }: { module: ModuleKey }) {
  const meta = MODULE_META[module];
  const locale = useLocaleStore((s) => s.locale);
  const title = CHAT_COPY[locale].welcome[module];

  return (
    <div className="flex flex-col items-center text-center">
      <div className="mb-4 flex size-14 items-center justify-center rounded-full bg-white shadow-md">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={meta.iconSrc} alt="" className="size-9 rounded-full" />
      </div>
      <h2 className="mb-6 text-2xl font-semibold">{title}</h2>
    </div>
  );
}
