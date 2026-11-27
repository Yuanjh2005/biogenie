import type { Locale } from "@/stores/locale";

const DEFAULT_PHONE_NICKNAME_PATTERN = /^手机用户(\d{4})$/;

/** Localize the system-generated nickname without changing user-defined nicknames. */
export function localizeUserNickname(
  nickname: string | null | undefined,
  locale: Locale,
): string {
  if (!nickname) return "";

  const match = nickname.match(DEFAULT_PHONE_NICKNAME_PATTERN);
  if (!match || locale !== "en") return nickname;

  return `Phone User ${match[1]}`;
}
