/**
 * 运行时配置。API 基址通过环境变量注入。
 * 生产链路：前端 → 后端 API → 百炼；前端禁止直连 dashscope。
 */
export const env = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api",
  /**
   * 仅在后端 AI 接口不可用时置 true。
   * 后端已提供 /api/ai/* 时必须为 false，走真实接口。
   */
  useMockChat: (process.env.NEXT_PUBLIC_USE_MOCK_CHAT ?? "false") === "true",
  /** 超过该字符数时，将输入自动转为 txt 附件发送 */
  autoTextAttachmentLength: 5000,
  /** 单文件大小上限：10MB */
  maxUploadBytes: 10 * 1024 * 1024,
  /** 单次对话附件数量上限 */
  maxUploadCount: 10,
};
