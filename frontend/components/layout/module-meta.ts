import { BookOpen, FlaskConical, Sparkles, type LucideIcon } from "lucide-react";
import type { ModuleKey } from "@/types/chat";

/** 模块图标与用于气泡/高亮的 tailwind 主题类（对应 globals.css 变量） */
export const MODULE_META: Record<
  ModuleKey,
  { icon: LucideIcon; iconSrc: string; textClass: string; bgClass: string; ringClass: string }
> = {
  "smart-drgf": {
    icon: BookOpen,
    iconSrc: "/images/home/smart-drgf-icon.svg",
    textClass: "text-[var(--module-smart-drgf)]",
    bgClass: "bg-[var(--module-smart-drgf)]",
    ringClass: "ring-[var(--module-smart-drgf)]",
  },
  "deep-pubmed": {
    icon: FlaskConical,
    iconSrc: "/images/home/deep-pubmed-icon.svg",
    textClass: "text-[var(--module-deep-pubmed)]",
    bgClass: "bg-[var(--module-deep-pubmed)]",
    ringClass: "ring-[var(--module-deep-pubmed)]",
  },
  "design-mcp": {
    icon: Sparkles,
    iconSrc: "/images/home/design-mcp-icon.svg",
    textClass: "text-[var(--module-design-mcp)]",
    bgClass: "bg-[var(--module-design-mcp)]",
    ringClass: "ring-[var(--module-design-mcp)]",
  },
};
