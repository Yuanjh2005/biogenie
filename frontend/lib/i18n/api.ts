import type { Locale } from "@/stores/locale";

export const API_COPY: Record<
  Locale,
  {
    network: string;
    requestFailed: string;
    status: Record<number, string>;
    fallback: string;
  }
> = {
  "zh-CN": {
    network: "网络异常，请检查网络后重试",
    requestFailed: "请求失败",
    fallback: "请求失败，请稍后重试",
    status: {
      400: "请求参数有误",
      401: "登录已失效，请重新登录",
      403: "没有访问权限",
      404: "服务暂不可用",
      429: "操作过于频繁，请稍后重试",
      500: "服务异常，请稍后重试",
      502: "服务异常，请稍后重试",
      503: "服务暂不可用，请稍后重试",
      504: "请求超时，请稍后重试",
    },
  },
  en: {
    network: "Network error. Please check your connection and try again.",
    requestFailed: "Request failed",
    fallback: "Request failed. Please try again later.",
    status: {
      400: "Invalid request",
      401: "Your session has expired. Please sign in again.",
      403: "You do not have access",
      404: "Service is currently unavailable",
      429: "Too many requests. Please try again later.",
      500: "Service error. Please try again later.",
      502: "Service error. Please try again later.",
      503: "Service is currently unavailable. Please try again later.",
      504: "The request timed out. Please try again later.",
    },
  },
};
