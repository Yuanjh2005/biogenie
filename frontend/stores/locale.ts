import { create } from "zustand";

export type Locale = "zh-CN" | "en";

const STORAGE_KEY = "bg_locale";

interface LocaleState {
  locale: Locale;
  hydrated: boolean;
  hydrate: () => void;
  setLocale: (locale: Locale) => void;
}

function readStoredLocale(): Locale {
  if (typeof window === "undefined") return "zh-CN";
  return window.localStorage.getItem(STORAGE_KEY) === "en" ? "en" : "zh-CN";
}

function applyDocumentLang(locale: Locale) {
  if (typeof document === "undefined") return;
  document.documentElement.lang = locale === "en" ? "en" : "zh-CN";
}

export const useLocaleStore = create<LocaleState>((set) => ({
  locale: "zh-CN",
  hydrated: false,

  hydrate: () => {
    if (typeof window === "undefined") {
      set({ hydrated: true });
      return;
    }
    const locale = readStoredLocale();
    applyDocumentLang(locale);
    set({ locale, hydrated: true });
  },

  setLocale: (locale) => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, locale);
    }
    applyDocumentLang(locale);
    set({ locale });
  },
}));
