import type { Locale } from "@/stores/locale";
import type { ChatMode, ModuleKey } from "@/types/chat";

export const CHAT_COPY: Record<
  Locale,
  {
    welcome: Record<ModuleKey, string>;
    placeholder: string;
    modes: Record<ChatMode, string>;
    selectMode: string;
    pubmedYearUnlimited: string;
    pubmedTypeUnlimited: string;
    textTooLarge: string;
    attachmentPrompt: string;
    uploadMax: (max: number) => string;
    uploadRemain: (max: number, remain: number) => string;
    fileTooLarge: (name: string) => string;
    uploadFailed: (name: string) => string;
    uploadFile: string;
    uploadHint: (max: number) => string;
    stop: string;
    send: string;
    loading: string;
    thinking: string;
    generateFailed: string;
    stopped: string;
    thoughts: string;
    viewOriginal: string;
    mindMap: string;
    dataInsight: string;
    removeAttachment: string;
    previewAttachment: string;
    previewLoading: string;
    previewFailed: string;
    previewUnsupported: string;
    openOriginal: string;
    copy: string;
    copyFailed: string;
    copied: string;
    edit: string;
    cancel: string;
    regenerate: string;
    like: string;
    dislike: string;
    share: string;
    shareNotReady: string;
    shareCopied: string;
    shareGenerated: string;
    shareFailed: string;
    copyManually: string;
    tableEmpty: string;
    tableCopied: string;
    copyTable: string;
    retrievingWeb: string;
    readWebPages: (n: number) => string;
    chatFailed: string;
    modeUnavailable: string;
    sessionExpired: string;
    chatServiceError: string;
    copyQuestion: (q: string) => string;
    copyAnswer: (a: string) => string;
    copyReferences: string;
  }
> = {
  "zh-CN": {
    welcome: {
      "smart-drgf": "欢迎使用 SmartDRGF",
      "deep-pubmed": "欢迎使用 DeepLiterMed",
      "design-mcp": "欢迎使用 DesignMCP",
    },
    placeholder: "开始你的提问吧…",
    modes: {
      fast: "快速模式",
      expert: "专家模式",
      web: "联网模式",
    },
    selectMode: "选择模式",
    pubmedYearUnlimited: "年限不限",
    pubmedTypeUnlimited: "文献类型不限",
    textTooLarge: "文字数量超限，生成的 TXT 文件不能超过 10MB",
    attachmentPrompt: "请读取并回答 TXT 附件中的内容。",
    uploadMax: (max) => `最多上传 ${max} 个文件`,
    uploadRemain: (max, remain) => `最多上传 ${max} 个文件，还可再选 ${remain} 个`,
    fileTooLarge: (name) => `「${name}」不能超过 10MB`,
    uploadFailed: (name) => `「${name}」上传失败`,
    uploadFile: "上传文件",
    uploadHint: (max) => `最多 ${max} 个，单个不超过 10MB`,
    stop: "停止生成",
    send: "发送",
    loading: "加载中…",
    thinking: "正在思考…",
    generateFailed: "生成失败，请稍后重试",
    stopped: "已停止生成",
    thoughts: "思考过程",
    viewOriginal: "查看原文",
    mindMap: "AI脑图速览",
    dataInsight: "数据解读",
    removeAttachment: "移除附件",
    previewAttachment: "预览附件",
    previewLoading: "正在加载文件内容…",
    previewFailed: "文件预览加载失败",
    previewUnsupported: "当前文件格式暂不支持在线预览",
    openOriginal: "打开原文件",
    copy: "复制",
    copyFailed: "复制失败",
    copied: "已复制",
    edit: "编辑并重新发送",
    cancel: "取消编辑",
    regenerate: "重新生成",
    like: "点赞",
    dislike: "点踩",
    share: "分享",
    shareNotReady: "会话尚未就绪，请稍后再分享",
    shareCopied: "分享链接已复制（12 小时内有效）",
    shareGenerated: "分享链接已生成（12 小时内有效）",
    shareFailed: "分享失败",
    copyManually: "复制失败，请手动复制链接",
    tableEmpty: "表格为空，无法复制",
    tableCopied: "表格已复制，可直接粘贴到 Excel",
    copyTable: "复制表格",
    retrievingWeb: "正在检索网页…",
    readWebPages: (n) => `已阅读 ${n} 个网页`,
    chatFailed: "对话失败，请稍后重试",
    modeUnavailable: "当前模式暂不可用",
    sessionExpired: "登录已失效，请重新登录",
    chatServiceError: "对话服务异常，请稍后重试",
    copyQuestion: (q) => `问题：${q}`,
    copyAnswer: (a) => `回答：${a}`,
    copyReferences: "参考文献：",
  },
  en: {
    welcome: {
      "smart-drgf": "Welcome to SmartDRGF",
      "deep-pubmed": "Welcome to DeepLiterMed",
      "design-mcp": "Welcome to DesignMCP",
    },
    placeholder: "Start your question…",
    modes: {
      fast: "Fast Mode",
      expert: "Expert Mode",
      web: "Web Search",
    },
    selectMode: "Select mode",
    pubmedYearUnlimited: "Any year",
    pubmedTypeUnlimited: "Any literature type",
    textTooLarge: "Text limit exceeded. The generated TXT file cannot exceed 10MB.",
    attachmentPrompt: "Please read and respond to the content in the TXT attachment.",
    uploadMax: (max) => `You can upload up to ${max} files`,
    uploadRemain: (max, remain) =>
      `You can upload up to ${max} files; ${remain} more can be selected`,
    fileTooLarge: (name) => `"${name}" cannot exceed 10MB`,
    uploadFailed: (name) => `Failed to upload "${name}"`,
    uploadFile: "Upload file",
    uploadHint: (max) => `Up to ${max} files, 10MB each`,
    stop: "Stop generating",
    send: "Send",
    loading: "Loading…",
    thinking: "Thinking…",
    generateFailed: "Generation failed. Please try again later.",
    stopped: "Generation stopped",
    thoughts: "Thinking process",
    viewOriginal: "View original",
    mindMap: "AI mind map",
    dataInsight: "Data insight",
    removeAttachment: "Remove attachment",
    previewAttachment: "Preview attachment",
    previewLoading: "Loading file content…",
    previewFailed: "Failed to load the file preview",
    previewUnsupported: "This file format cannot be previewed online",
    openOriginal: "Open original file",
    copy: "Copy",
    copyFailed: "Copy failed",
    copied: "Copied",
    edit: "Edit and resend",
    cancel: "Cancel editing",
    regenerate: "Regenerate",
    like: "Like",
    dislike: "Dislike",
    share: "Share",
    shareNotReady: "The conversation is not ready yet. Please share later.",
    shareCopied: "Share link copied (valid for 12 hours)",
    shareGenerated: "Share link generated (valid for 12 hours)",
    shareFailed: "Share failed",
    copyManually: "Copy failed. Please copy the link manually.",
    tableEmpty: "The table is empty and cannot be copied",
    tableCopied: "Table copied. You can paste it into Excel.",
    copyTable: "Copy table",
    retrievingWeb: "Searching the web…",
    readWebPages: (n) => `Read ${n} web page${n === 1 ? "" : "s"}`,
    chatFailed: "Chat failed. Please try again later.",
    modeUnavailable: "This mode is currently unavailable",
    sessionExpired: "Your session has expired. Please sign in again.",
    chatServiceError: "The chat service is unavailable. Please try again later.",
    copyQuestion: (q) => `Question: ${q}`,
    copyAnswer: (a) => `Answer: ${a}`,
    copyReferences: "References:",
  },
};
