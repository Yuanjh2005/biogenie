// HTTP / 业务错误映射为用户可读文案（不暴露堆栈与内网地址）。

import { API_COPY } from "@/lib/i18n/api";
import { useLocaleStore } from "@/stores/locale";

export class ApiError extends Error {
  status: number;
  code?: number | string;
  constructor(message: string, status: number, code?: number | string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

/** 401 时触发的全局回调（由 auth store 注册），用于清登录态并跳转登录页。 */
let onUnauthorized: (() => void) | null = null;
export function registerUnauthorizedHandler(fn: () => void): void {
  onUnauthorized = fn;
}
export function triggerUnauthorized(): void {
  onUnauthorized?.();
}

export function messageForStatus(status: number, fallback?: string): string {
  const copy = API_COPY[useLocaleStore.getState().locale];
  return copy.status[status] ?? fallback ?? copy.fallback;
}
