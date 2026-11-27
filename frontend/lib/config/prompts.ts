import type { ModuleKey } from "@/types/chat";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { HOME_COPY } from "@/lib/i18n/home";
import type { Locale } from "@/stores/locale";

/** 各模块欢迎语与推荐问题（随界面语言切换） */
export function moduleWelcome(module: ModuleKey, locale: Locale) {
  return {
    title: CHAT_COPY[locale].welcome[module],
    recommended: HOME_COPY[locale].modules[module].recommended,
  };
}
