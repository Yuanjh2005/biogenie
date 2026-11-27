// 对话 / 会话的 mock 层。后端接口就绪后由 env.useMockChat=false 切换到真实实现。

import type {
  ChatCompletionParams,
  ChatStreamEvent,
  Conversation,
  DocReference,
  StructuredTable,
} from "@/types/chat";

function delay(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}

const SAMPLE_TEXT =
  "根据现有研究，细胞生长因子在组织修复中的作用主要体现在促进细胞增殖、调节炎症微环境以及增强血管新生三个方面。\n\n" +
  "1. **促增殖**：通过激活 PI3K/AKT 通路提升成纤维细胞活性。\n" +
  "2. **抗炎调节**：下调促炎因子 IL-6、TNF-α 的表达。\n" +
  "3. **血管新生**：上调 VEGF，促进创面血流恢复。\n\n" +
  "综上，细胞生长因子在慢性创面修复中具有明确的临床应用潜力。";

const SAMPLE_THOUGHTS =
  "用户询问细胞生长因子的作用机制。我需要从增殖、炎症、血管三个维度组织答案，并给出关键信号通路与代表性因子，最后给出临床结论。";

const SAMPLE_REFS: DocReference[] = [
  {
    index: 1,
    title: "Growth factors in wound healing: a systematic review",
    journal: "J. Tissue Repair",
    year: 2023,
    conclusion: "生长因子联合应用可显著缩短慢性创面愈合时间。",
    url: "https://pubmed.ncbi.nlm.nih.gov/",
  },
  {
    index: 2,
    title: "VEGF signaling in angiogenesis during tissue regeneration",
    journal: "Cell Regen.",
    year: 2022,
    conclusion: "VEGF 通路是血管新生的核心调控节点。",
    url: "https://pubmed.ncbi.nlm.nih.gov/",
  },
];

const SAMPLE_TABLE: StructuredTable = {
  columns: ["评估维度", "支持证据"],
  rows: [
    ["安全性", "多中心随机对照试验未见严重不良反应"],
    ["有效性", "创面愈合率提升约 28%"],
    ["可及性", "可常温冻干保存，便于基层推广"],
  ],
};

/** mock 流式输出：逐字/逐段返回增量事件 */
export async function* mockChatStream(
  params: ChatCompletionParams,
  signal?: AbortSignal,
): AsyncGenerator<ChatStreamEvent> {
  const sessionId = params.sessionId ?? `mock-session-${Date.now()}`;
  yield { sessionId };

  if (params.mode === "expert") {
    for (const ch of SAMPLE_THOUGHTS) {
      if (signal?.aborted) return;
      await delay(12);
      yield { thoughts: ch };
    }
  }

  if (params.module === "deep-pubmed") {
    yield { text: `已为你检索到 ${SAMPLE_REFS.length} 篇相关文献：\n\n` };
  }

  if (params.mode === "web") {
    yield { webPageCount: 6, text: "正在阅读相关网页…\n\n" };
  }

  for (const ch of SAMPLE_TEXT) {
    if (signal?.aborted) {
      yield { finishReason: "stopped" };
      return;
    }
    await delay(10);
    yield { text: ch };
  }

  if (params.module === "deep-pubmed") {
    yield { docReferences: SAMPLE_REFS };
  }

  if (params.mode === "web") {
    yield {
      webPageCount: 6,
      text: "\n\n来源于网络，请注意甄别。",
    };
  }

  if (params.module === "design-mcp") {
    yield { table: SAMPLE_TABLE };
  }

  yield { finishReason: "stop" };
}

export function mockStructuredTable(): StructuredTable {
  return SAMPLE_TABLE;
}

let mockConvSeq = 0;
export function mockCreateConversation(
  module: Conversation["module"],
  title: string,
): Conversation {
  mockConvSeq += 1;
  return {
    id: `mock-conv-${Date.now()}-${mockConvSeq}`,
    module,
    title,
    updatedAt: Date.now(),
  };
}
