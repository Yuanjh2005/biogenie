import type { Locale } from "@/stores/locale";

export const SHARE_COPY: Record<
  Locale,
  {
    readonlyHint: string;
    cannotView: string;
    backHome: string;
    openFailed: string;
    invalidLink: string;
    loading: string;
    fallbackTitle: string;
    expiresAt: (value: string) => string;
    attachment: string;
  }
> = {
  "zh-CN": {
    readonlyHint: "只读分享 · 12 小时有效",
    cannotView: "无法查看分享",
    backHome: "回到首页",
    openFailed: "无法打开分享",
    invalidLink: "分享链接无效",
    loading: "加载分享内容…",
    fallbackTitle: "对话分享",
    expiresAt: (value) => `有效期至 ${value}`,
    attachment: "附件",
  },
  en: {
    readonlyHint: "Read-only share · valid for 12 hours",
    cannotView: "Unable to view this share",
    backHome: "Back to home",
    openFailed: "Unable to open this share",
    invalidLink: "This share link is invalid",
    loading: "Loading shared conversation…",
    fallbackTitle: "Shared conversation",
    expiresAt: (value) => `Expires ${value}`,
    attachment: "Attachment",
  },
};
