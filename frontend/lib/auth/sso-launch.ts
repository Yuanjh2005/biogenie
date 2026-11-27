import type { Attachment, ModuleKey } from "@/types/chat";

const STORAGE_KEY = "biogenie:sso-launch";

interface SsoLaunch {
  module: ModuleKey;
  question?: string;
  attachments?: Attachment[];
}

export function saveSsoLaunch(launch: SsoLaunch): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(launch));
  } catch {
    // 浏览器禁用会话存储时仍允许完成 SSO 登录，只是不再自动发送问题。
  }
}

export function readSsoLaunch(module: ModuleKey): SsoLaunch | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const launch = JSON.parse(raw) as Partial<SsoLaunch>;
    if (launch.module !== module) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    const question = typeof launch.question === "string" ? launch.question.trim() : undefined;
    const attachments = Array.isArray(launch.attachments)
      ? launch.attachments.filter((item): item is Attachment =>
          Boolean(item && typeof item.url === "string" && typeof item.name === "string"))
      : undefined;
    if (!question && !attachments?.length) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return { module, question, attachments };
  } catch {
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Ignore storage access errors.
    }
    return null;
  }
}

export function clearSsoLaunch(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore storage access errors.
  }
}
