// 对话领域模型（与后端 /api/ai、/api/file 联调对齐）

export type ModuleKey = "smart-drgf" | "deep-pubmed" | "design-mcp";

export type ChatMode = "fast" | "expert" | "web"; // 快速 / 专家 / 联网

export type MessageStatus =
  | "pending"
  | "streaming"
  | "done"
  | "stopped"
  | "error";

export type MessageRole = "user" | "assistant";

/** 文献引用（DeepPubMed / 通用 doc_references） */
export interface DocReference {
  id?: string;
  title: string;
  citation?: string; // 完整引文：作者、标题、期刊、年份、PMID 等
  journal?: string;
  year?: string | number;
  conclusion?: string; // 核心结论
  url?: string; // 查看原文
  index?: number;
}

/** 上传附件（DesignMCP） */
export interface Attachment {
  id: string;
  name: string;
  size: number;
  type: string;
  url?: string; // 上传后的文件 URL
  /** 当前页面选择的原始文件，仅用于本地预览，不会发送给后端 */
  sourceFile?: File;
  status: "uploading" | "done" | "error";
}

/** 结构化表格（DesignMCP：维度 / 支持证据） */
export interface StructuredTable {
  columns: string[];
  rows: string[][];
}

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  status: MessageStatus;
  mode?: ChatMode;
  thoughts?: string; // 专家模式思考过程
  docReferences?: DocReference[];
  table?: StructuredTable;
  attachments?: Attachment[];
  retrievalSummary?: string; // 联网「已阅读 N 个网页」/ DeepPubMed 检索摘要
  webPageCount?: number;
  shareId?: string;
  finishReason?: string;
  createdAt: number;
}

export interface Conversation {
  id: string;
  module: ModuleKey;
  title: string;
  sessionId?: string; // 百炼多轮 session_id
  updatedAt: number;
}

/** 分组后的会话历史 */
export interface ConversationGroups {
  today: Conversation[];
  yesterday: Conversation[];
  earlier: Conversation[];
}

/** 发送对话的请求参数（前端内部形态） */
export interface ChatCompletionParams {
  module: ModuleKey;
  mode: ChatMode;
  prompt: string;
  sessionId?: string;
  attachments?: Attachment[];
  redesignOption?: string; // DesignMCP：rational / de-novo
  pubmedYear?: string;
  pubmedArticleType?: string;
}

/** SSE 增量事件 */
export interface ChatStreamEvent {
  text?: string;
  thoughts?: string;
  sessionId?: string;
  docReferences?: DocReference[];
  table?: StructuredTable;
  finishReason?: string;
  webPageCount?: number;
}
