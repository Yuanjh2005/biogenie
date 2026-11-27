import type { ChatMode, ModuleKey } from "@/types/chat";

/**
 * Agent 配置表：后端 ai.apps 配置名 + 模式参数。
 * 真实 appId / apiKey 由后端持有，前端只传 app 配置名。
 */
export interface AgentModeConfig {
  /** 后端 application.yml 中 ai.apps 的 key */
  app: string;
  enableThinking?: boolean;
  hasThoughts?: boolean;
}

export interface ModuleConfig {
  key: ModuleKey;
  label: string;
  /** 主题色 CSS 变量名（见 globals.css） */
  themeVar: string;
  route: string;
  modes: ChatMode[];
  agents: Partial<Record<ChatMode, AgentModeConfig>>;
}

const EXPERT_FLAGS = { enableThinking: true, hasThoughts: true } as const;

export const MODULES: Record<ModuleKey, ModuleConfig> = {
  "smart-drgf": {
    key: "smart-drgf",
    label: "SmartDRGF",
    themeVar: "--color-module-smart-drgf",
    route: "/chat/smart-drgf",
    modes: ["fast", "expert", "web"],
    agents: {
      fast: { app: "smartdrgf-fast" },
      expert: { app: "smartdrgf-fast", ...EXPERT_FLAGS },
      web: { app: "smartdrgf-web" },
    },
  },
  "deep-pubmed": {
    key: "deep-pubmed",
    label: "DeepLiterMed",
    themeVar: "--color-module-deep-pubmed",
    route: "/chat/deep-pubmed",
    modes: ["fast", "expert"],
    agents: {
      fast: { app: "deep-pubmed" },
      expert: { app: "deep-pubmed", ...EXPERT_FLAGS },
    },
  },
  "design-mcp": {
    key: "design-mcp",
    label: "DesignMCP",
    themeVar: "--color-module-design-mcp",
    route: "/chat/design-mcp",
    modes: ["fast", "expert"],
    agents: {
      fast: { app: "design-mcp" },
      expert: { app: "design-mcp", ...EXPERT_FLAGS },
    },
  },
};

export function getAgent(module: ModuleKey, mode: ChatMode): AgentModeConfig | undefined {
  return MODULES[module].agents[mode];
}

/** 后端 appKey → 前端模块 */
export function moduleFromAppKey(appKey?: string | null): ModuleKey {
  if (appKey?.startsWith("deep-pubmed")) return "deep-pubmed";
  if (appKey?.startsWith("design-mcp")) return "design-mcp";
  // 兼容 app_key 为空或使用旧配置名的历史数据，避免整条会话被静默丢弃。
  return "smart-drgf";
}

export const MODULE_LIST = Object.values(MODULES);

/** 根据当前路径解析模块；首页与 DRGF 复用 SmartDRGF */
export function moduleFromPath(pathname: string): ModuleKey | undefined {
  const fromChat = MODULE_LIST.find((m) => pathname.startsWith(m.route))?.key;
  if (fromChat) return fromChat;
  if (pathname.startsWith("/home") || pathname.startsWith("/drgf")) return "smart-drgf";
  return undefined;
}

/** DRGF 页展示 SmartDRGF 内容时，会话跳转停留在 /drgf */
export function routeForModule(module: ModuleKey, pathname: string): string {
  if (pathname.startsWith("/drgf") && module === "smart-drgf") return "/drgf";
  return MODULES[module].route;
}
