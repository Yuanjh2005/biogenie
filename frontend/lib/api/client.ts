import { env } from "@/lib/config/env";
import { getRefreshToken, getToken } from "@/lib/auth/token";
import {
  captureRenewedAccessToken,
  refreshAccessToken,
} from "@/lib/auth/refresh";
import type { ApiResult } from "@/types/api";
import {
  ApiError,
  messageForStatus,
  triggerUnauthorized,
} from "./errors";
import { API_COPY } from "@/lib/i18n/api";
import { useLocaleStore } from "@/stores/locale";

type Query = Record<string, string | number | boolean | undefined | null>;

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  query?: Query;
  /** JSON body */
  body?: unknown;
  /** 原始 body（FormData 等），设置后忽略 body */
  raw?: BodyInit;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  /** 是否需要鉴权头，默认 true */
  auth?: boolean;
}

function buildUrl(path: string, query?: Query): string {
  const base = env.apiBaseUrl.replace(/\/$/, "");
  const url = `${base}${path.startsWith("/") ? path : `/${path}`}`;
  if (!query) return url;
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null) qs.append(k, String(v));
  }
  const s = qs.toString();
  return s ? `${url}?${s}` : url;
}

function authHeaders(enabled: boolean): Record<string, string> {
  if (!enabled) return {};
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/** 发起请求并拆解统一响应包 `{ code, message, data }`，返回 data。 */
export async function request<T>(
  path: string,
  options: RequestOptions = {},
  canRetryAuth = true,
): Promise<T> {
  const { method = "GET", query, body, raw, headers = {}, signal, auth = true } = options;

  const isForm = raw instanceof FormData;
  const finalHeaders: Record<string, string> = {
    ...(!isForm && (body !== undefined) ? { "Content-Type": "application/json" } : {}),
    ...authHeaders(auth),
    ...headers,
  };

  let res: Response;
  try {
    res = await fetch(buildUrl(path, query), {
      method,
      headers: finalHeaders,
      body: raw ?? (body !== undefined ? JSON.stringify(body) : undefined),
      signal,
    });
  } catch (e) {
    if ((e as Error)?.name === "AbortError") throw e;
    throw new ApiError(API_COPY[useLocaleStore.getState().locale].network, 0);
  }

  captureRenewedAccessToken(res);

  let payload: ApiResult<T> | null = null;
  try {
    payload = (await res.json()) as ApiResult<T>;
  } catch {
    // 非 JSON 响应
  }

  const code = payload?.code != null ? Number(payload.code) : NaN;
  const accessRejected =
    auth && (res.status === 401 || code === 40100 || code === 40101);

  if (accessRejected && canRetryAuth && getRefreshToken()) {
    try {
      await refreshAccessToken();
      return request<T>(path, options, false);
    } catch (error) {
      // 网络或服务异常不应清除仍可能有效的登录态。
      if (!(error instanceof ApiError) || error.status !== 401) throw error;
    }
  }

  if (accessRejected || code === 40102) {
    triggerUnauthorized();
    throw new ApiError(payload?.message || messageForStatus(401), 401, payload?.code);
  }

  if (!res.ok) {
    throw new ApiError(payload?.message || messageForStatus(res.status), res.status, payload?.code);
  }

  // 后端成功码为 20000；文档示例偶发写 200，一并兼容
  if (payload && !Number.isNaN(code) && code !== 20000 && code !== 200) {
    throw new ApiError(
      payload.message || API_COPY[useLocaleStore.getState().locale].requestFailed,
      res.status,
      payload.code,
    );
  }

  return (payload?.data as T) ?? (undefined as T);
}

export const api = {
  get: <T>(path: string, opts?: Omit<RequestOptions, "method" | "body">) =>
    request<T>(path, { ...opts, method: "GET" }),
  post: <T>(path: string, body?: unknown, opts?: Omit<RequestOptions, "method">) =>
    request<T>(path, { ...opts, method: "POST", body }),
  put: <T>(path: string, body?: unknown, opts?: Omit<RequestOptions, "method">) =>
    request<T>(path, { ...opts, method: "PUT", body }),
  delete: <T>(path: string, opts?: Omit<RequestOptions, "method">) =>
    request<T>(path, { ...opts, method: "DELETE" }),
  download: async (path: string, opts?: { query?: Query; headers?: Record<string, string> }) => {
    const res = await fetch(buildUrl(path, opts?.query), {
      headers: { ...authHeaders(true), ...opts?.headers },
    });
    captureRenewedAccessToken(res);
    if (!res.ok) throw new ApiError(messageForStatus(res.status), res.status);
    return {
      blob: await res.blob(),
      disposition: res.headers.get("Content-Disposition"),
    };
  },
};
