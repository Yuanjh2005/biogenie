import type { Locale } from "@/stores/locale";
import type { ModuleKey } from "@/types/chat";

export interface HomeModuleCopy {
  description: string;
  bullets: string[];
  buttonLabel: string;
  recommended: string[];
}

export const HOME_COPY: Record<
  Locale,
  {
    title: string;
    subtitle: string;
    placeholder: string;
    tryQuestions: string;
    modules: Record<ModuleKey, HomeModuleCopy>;
  }
> = {
  "zh-CN": {
    title: "AI驱动的生物医学发现平台",
    subtitle: "整合知识发现、证据挖掘与分子设计的多智能体系统",
    placeholder: "开始你的提问吧...",
    tryQuestions: "试试这些问题：",
    modules: {
      "smart-drgf": {
        description: "细胞生长因子知识智能问答",
        bullets: ["全面的生长因子知识库", "机制、功能、应用全覆盖", "智能问答，精准解答"],
        buttonLabel: "开始探索",
        recommended: [
          "EGF 在皮肤修复中的作用机制",
          "FGF 家族成员的功能比较",
          "最新的生长因子临床研究进展",
        ],
      },
      "deep-pubmed": {
        description: "智能化文献检索与解读",
        bullets: ["PubMed 海量文献检索", "AI 摘要与关键点提取", "深度分析与可视化"],
        buttonLabel: "开始探索",
        recommended: [
          "检索近三年 VEGF 与创面愈合的高质量文献",
          "CGF 在骨再生中的临床证据",
          "生长因子联合治疗的 meta 分析",
        ],
      },
      "design-mcp": {
        description: "端到端蛋白药物设计平台",
        bullets: ["从靶点到候选的全流程设计", "多模态 AI 驱动优化", "高效预测与验证"],
        buttonLabel: "开始设计",
        recommended: [
          "上传方案文档并给出结构化评估",
          "从循证角度设计一个创面修复方案",
          "对比两种生长因子给药方案的优劣",
        ],
      },
    },
  },
  en: {
    title: "AI-powered Platform for Biomedical Discovery",
    subtitle:
      "A multi-agent system integrating knowledge discovery, evidence mining, and molecular design",
    placeholder: "Start your question...",
    tryQuestions: "Try these questions:",
    modules: {
      "smart-drgf": {
        description: "AI-powered Q&A for growth factors",
        bullets: [
          "Comprehensive growth factor knowledge base",
          "Mechanisms functions and applications",
          "Precise answers powered by AI",
        ],
        buttonLabel: "Start exploring",
        recommended: [
          "3D structures and PTM information for FGF1 and FGF2",
          "Human FGF family members and their sequences",
          "VEGF-related patents",
        ],
      },
      "deep-pubmed": {
        description: "AI-powered literature intelligence",
        bullets: [
          "Search millions of peer-reviewed publications",
          "Extract key findings with AI",
          "In-depth analysis and visualization",
        ],
        buttonLabel: "Start exploring",
        recommended: [
          "What are the 3D structures and PTMs of FGF1 and FGF2?",
          "What are the human FGF family members and their sequences?",
          "What patents are related to VEGF?",
        ],
      },
      "design-mcp": {
        description: "Intelligent protein design guidance",
        bullets: [
          "Literature-driven design strategies",
          "Target-specific design pathways and workflows",
          "AI-guided design and optimization",
        ],
        buttonLabel: "Start designing",
        recommended: [
          "Upload a protocol and give a structured assessment",
          "Design a wound-repair protocol from an evidence-based view",
          "Compare two growth factor dosing regimens",
        ],
      },
    },
  },
};
