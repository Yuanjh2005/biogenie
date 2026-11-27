import type { Locale } from "@/stores/locale";

export const SHELL_COPY: Record<
  Locale,
  {
    home: string;
    newChat: string;
    conversationHistory: string;
    searchConversations: string;
    clearSearch: string;
    noSearchResults: (keyword: string) => string;
    noConversations: string;
    conversationsLoadFailed: string;
    retry: string;
    today: string;
    yesterday: string;
    earlier: string;
    navMenu: string;
    openMenu: string;
    settings: string;
    userFallback: string;
    loading: string;
    deleteConversation: string;
    untitledConversation: string;
    attachment: string;
  }
> = {
  "zh-CN": {
    home: "首页",
    newChat: "新对话",
    conversationHistory: "历史会话",
    searchConversations: "搜索历史会话",
    clearSearch: "清除搜索",
    noSearchResults: (keyword) => `未找到与“${keyword}”相关的会话`,
    noConversations: "暂无会话记录",
    conversationsLoadFailed: "历史会话加载失败",
    retry: "重新加载",
    today: "今天",
    yesterday: "昨天",
    earlier: "更早",
    navMenu: "导航菜单",
    openMenu: "打开菜单",
    settings: "系统设置",
    userFallback: "用户",
    loading: "加载中…",
    deleteConversation: "删除对话",
    untitledConversation: "未命名会话",
    attachment: "附件",
  },
  en: {
    home: "Home",
    newChat: "New chat",
    conversationHistory: "History",
    searchConversations: "Search conversations",
    clearSearch: "Clear search",
    noSearchResults: (keyword) => `No conversations found for “${keyword}”`,
    noConversations: "No conversations yet",
    conversationsLoadFailed: "Failed to load conversation history",
    retry: "Retry",
    today: "Today",
    yesterday: "Yesterday",
    earlier: "Earlier",
    navMenu: "Navigation",
    openMenu: "Open menu",
    settings: "Settings",
    userFallback: "User",
    loading: "Loading…",
    deleteConversation: "Delete conversation",
    untitledConversation: "Untitled conversation",
    attachment: "Attachment",
  },
};
