import { env } from "@/lib/config/env";
import { getRefreshToken, setAccessToken } from "@/lib/auth/token";
import { API_COPY } from "@/lib/i18n/api";
import { useLocaleStore } from "@/stores/locale";
import type { ApiResult, TokenRefreshResultDTO } from "@/types/api";
import { ApiError, messageForStatus } from "@/lib/api/errors";

export const RENEWED_ACCESS_TOKEN_HEADER = "X-Access-Token";

let refreshPromise: Promise<string> | null = null;

/** 保存后端在普通业务响应中无感续签的 Access Token。 */
export function captureRenewedAccessToken(response: Response): void {
  const token = response.headers.get(RENEWED_ACCESS_TOKEN_HEADER);
  if (token) setAccessToken(token);
}

async function performRefresh(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new ApiError(messageForStatus(401), 401, 40100);
  }

  let response: Response;
  try {
    response = await fetch(`${env.apiBaseUrl.replace(/\/$/, "")}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  } catch (error) {
    if ((error as Error)?.name === "AbortError") throw error;
    throw new ApiError(API_COPY[useLocaleStore.getState().locale].network, 0);
  }

  let payload: ApiResult<TokenRefreshResultDTO> | null = null;
  try {
    payload = (await response.json()) as ApiResult<TokenRefreshResultDTO>;
  } catch {
    // 交由下面的统一错误分支处理。
  }

  const code = payload?.code != null ? Number(payload.code) : NaN;
  const successCode = code === 20000 || code === 200;
  const token = payload?.data?.token;

  if (!response.ok || !successCode || !token) {
    const authFailure =
      response.status === 401 || code === 40100 || code === 40101 || code === 40102;
    throw new ApiError(
      payload?.message || messageForStatus(authFailure ? 401 : response.status),
      authFailure ? 401 : response.status,
      payload?.code,
    );
  }

  setAccessToken(token);
  return token;
}

/**
 * 刷新 Access Token。并发请求共享同一次刷新，避免 Token 过期时请求风暴。
 */
export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}
