// 登录态存储。token 不进 URL / 不打印日志。此处用 localStorage 以支持刷新恢复；
// 若后端改为 httpOnly cookie，可将读写切换为无操作并依赖 cookie。

import type { UserInfoDTO } from "@/types/api";

const TOKEN_KEY = "bg_token";
const REFRESH_TOKEN_KEY = "bg_refresh_token";
const SESSION_KEY = "bg_session_id";
const USER_KEY = "bg_user";

const isBrowser = typeof window !== "undefined";

export function getToken(): string | null {
  if (!isBrowser) return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (!isBrowser) return null;
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getSessionId(): string | null {
  if (!isBrowser) return null;
  return window.localStorage.getItem(SESSION_KEY);
}

export function getStoredUser(): UserInfoDTO | null {
  if (!isBrowser) return null;
  const raw = window.localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserInfoDTO;
  } catch {
    return null;
  }
}

export function setAuth(
  token: string,
  sessionId: string,
  user: UserInfoDTO,
  refreshToken?: string,
): void {
  if (!isBrowser) return;
  window.localStorage.setItem(TOKEN_KEY, token);
  window.localStorage.setItem(SESSION_KEY, sessionId);
  window.localStorage.setItem(USER_KEY, JSON.stringify(user));
  if (refreshToken) {
    window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function setAccessToken(token: string): void {
  if (!isBrowser) return;
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function updateStoredUser(user: UserInfoDTO): void {
  if (!isBrowser) return;
  window.localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuth(): void {
  if (!isBrowser) return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.localStorage.removeItem(SESSION_KEY);
  window.localStorage.removeItem(USER_KEY);
}
