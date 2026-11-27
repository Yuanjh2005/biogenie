import { api } from "./client";
import type {
  LoginResultDTO,
  TokenRefreshResultDTO,
  UserSessionDTO,
  WechatQrConfigDTO,
} from "@/types/api";

export const authApi = {
  sendSmsCode: (phone: string) =>
    api.post<string>("/auth/send-code", { phone }, { auth: false }),

  login: (phone: string, code: string) =>
    api.post<LoginResultDTO>("/auth/login", { phone, code }, { auth: false }),

  loginByPassword: (username: string, password: string) =>
    api.post<LoginResultDTO>("/auth/login/password", { username, password }, { auth: false }),

  registerByPassword: (username: string, password: string) =>
    api.post<LoginResultDTO>("/auth/register/password", { username, password }, { auth: false }),

  getWechatQrConfig: () =>
    api.get<WechatQrConfigDTO>("/auth/wechat/qrcode", { auth: false }),

  loginByWechat: (code: string, state: string) =>
    api.post<LoginResultDTO>("/auth/login/wechat", { code, state }, { auth: false }),

  loginBySso: (ticket: string) =>
    api.post<LoginResultDTO>("/auth/login/sso", { ticket }, { auth: false }),

  refresh: (refreshToken: string) =>
    api.post<TokenRefreshResultDTO>(
      "/auth/refresh",
      { refreshToken },
      { auth: false },
    ),

  sessions: () => api.get<UserSessionDTO[]>("/auth/sessions"),

  sessionCount: () => api.get<number>("/auth/sessions/count"),

  logout: () => api.post<string>("/auth/logout"),

  kickSession: (sessionId: string) =>
    api.delete<string>(`/auth/sessions/${sessionId}`),
};
