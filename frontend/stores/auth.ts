import { create } from "zustand";
import type { LoginResultDTO, UserInfoDTO } from "@/types/api";
import {
  clearAuth,
  getStoredUser,
  getToken,
  setAuth,
  updateStoredUser,
} from "@/lib/auth/token";

interface AuthState {
  user: UserInfoDTO | null;
  isAuthenticated: boolean;
  hydrated: boolean;
  /** 从本地存储恢复登录态（刷新后调用） */
  hydrate: () => void;
  /** 登录成功后写入 */
  setSession: (result: LoginResultDTO) => void;
  setUser: (user: UserInfoDTO) => void;
  /** 清除登录态（登出 / 401） */
  reset: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  hydrated: false,

  hydrate: () => {
    const token = getToken();
    const user = getStoredUser();
    set({ user, isAuthenticated: Boolean(token), hydrated: true });
  },

  setSession: (result) => {
    setAuth(result.token, result.sessionId, result.userInfo, result.refreshToken);
    set({ user: result.userInfo, isAuthenticated: true });
  },

  setUser: (user) => {
    updateStoredUser(user);
    set({ user });
  },

  reset: () => {
    clearAuth();
    set({ user: null, isAuthenticated: false });
  },
}));
