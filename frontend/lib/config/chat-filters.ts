import type { Locale } from "@/stores/locale";

export const DESIGN_MODES = ["rational", "de-novo"] as const;
export type DesignMode = (typeof DESIGN_MODES)[number];

export const DESIGN_MODE_LABELS: Record<Locale, Record<DesignMode, string>> = {
  "zh-CN": {
    rational: "理性设计",
    "de-novo": "从头设计",
  },
  en: {
    rational: "Rational Design",
    "de-novo": "De novo design",
  },
};

/** 传给模型的固定中英对照，不随界面语言切换 */
export function designModePromptHint(mode: string): string | undefined {
  if (mode === "rational") return "理性设计（Rational Design）";
  if (mode === "de-novo") return "从头设计（De novo design）";
  return mode || undefined;
}

export const PUBMED_YEAR_OPTIONS = [
  { value: "unlimited" },
  { value: "5y" },
  { value: "10y" },
] as const;

export const PUBMED_TYPE_OPTIONS = [
  { value: "unlimited" },
  { value: "research" },
  { value: "review" },
  { value: "clinical" },
] as const;

export type PubmedYear = (typeof PUBMED_YEAR_OPTIONS)[number]["value"];
export type PubmedArticleType = (typeof PUBMED_TYPE_OPTIONS)[number]["value"];

export const PUBMED_YEAR_LABELS: Record<Locale, Record<PubmedYear, string>> = {
  "zh-CN": {
    unlimited: "年限不限",
    "5y": "近五年",
    "10y": "近十年",
  },
  en: {
    unlimited: "Any year",
    "5y": "Past 5 years",
    "10y": "Past 10 years",
  },
};

export const PUBMED_TYPE_LABELS: Record<Locale, Record<PubmedArticleType, string>> = {
  "zh-CN": {
    unlimited: "文献类型不限",
    research: "研究论文",
    review: "综述",
    clinical: "临床试验",
  },
  en: {
    unlimited: "Any literature type",
    research: "Research article",
    review: "Review",
    clinical: "Clinical trial",
  },
};

export function pubmedYearConstraint(year: PubmedYear): string | undefined {
  if (year === "5y") return "仅检索近五年（最近 5 年）发表的文献";
  if (year === "10y") return "仅检索近十年（最近 10 年）发表的文献";
  return undefined;
}

export function pubmedTypeConstraint(type: PubmedArticleType): string | undefined {
  if (type === "research") return "文献类型限定为研究论文（original research），不要返回综述或临床试验注册条目";
  if (type === "review") return "文献类型限定为综述（review / systematic review）";
  if (type === "clinical") return "文献类型限定为临床试验（clinical trial）";
  return undefined;
}
