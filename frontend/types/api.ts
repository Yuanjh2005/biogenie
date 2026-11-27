// 后端统一响应包与 DTO 类型，来源：frontend/docs/温州医科大.md

export interface ApiResult<T> {
  code: number | string;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  total: number;
  page: number;
  size: number;
  records: T[];
}

// 用户
export interface UserInfoDTO {
  id: number;
  nickname: string;
  avatar: string;
  phone: string;
  gender: number; // 0未知 1男 2女
  role?: string; // USER / ADMIN / SUPER_ADMIN
}

export interface UpdateUserDTO {
  nickname?: string;
  gender?: number;
}

// 用户管理（管理员）
export interface UserRecordDTO {
  id: number;
  phone: string;
  nickname: string;
  avatar: string;
  gender: number;
  status: number; // 状态: 0禁用 1正常
  role?: string;
  lastLoginTime?: string;
  adminGrantedBy?: number;
  adminGrantedByName?: string;
  adminGrantedAt?: string;
  adminRemark?: string;
  createTime?: string;
}

export interface SecondFactorTokenDTO {
  confirmToken: string;
  expiresInSeconds: number;
}

export interface OperationsOverviewDTO {
  days: number;
  from: string;
  totalUsers: number;
  enabledUsers: number;
  newUsers: number;
  activeUsers: number;
  admins: number;
  totalConversations: number;
  newConversations: number;
  messages: number;
  inputTokens: number;
  outputTokens: number;
}

export interface AdminConversationSessionDTO {
  sessionId: string;
  userId: number;
  userNickname?: string;
  userPhone?: string;
  title?: string;
  appKey?: string;
  status: number;
  createTime?: string;
  updateTime?: string;
}

export interface AdminAuditLogDTO {
  id: number;
  operatorId: number;
  action: string;
  targetType?: string;
  targetId?: string;
  detail?: string;
  ip?: string;
  createTime: string;
}

export interface ApiBehaviorLogDTO {
  id: number;
  userId: number;
  userRole: string;
  requestMethod: string;
  requestPath: string;
  statusCode: number;
  durationMs: number;
  ip?: string;
  userAgent?: string;
  createTime: string;
}

export interface UserPageQuery {
  current: number;
  size: number;
  keyword?: string;
}

// 认证
export interface LoginDTO {
  phone: string;
  code: string;
}

export interface PasswordLoginDTO {
  username: string;
  password: string;
}

export interface WechatLoginDTO {
  code: string;
  state: string;
}

export interface SsoLoginDTO {
  ticket: string;
}

export interface SmsCodeDTO {
  phone: string;
}

export interface LoginResultDTO {
  token: string;
  refreshToken?: string;
  sessionId: string;
  deviceCount: number;
  expireTime: number; // 毫秒
  refreshExpireTime?: number;
  userInfo: UserInfoDTO;
  targetModule?: "smart-drgf" | "deep-pubmed" | "design-mcp";
  question?: string;
  attachments?: ChatAttachmentDTO[];
}

export interface TokenRefreshResultDTO {
  token: string;
  expireTime: number;
}

/** AI 会话列表项 */
export interface ConversationSessionDTO {
  sessionId: string;
  title: string;
  appKey?: string | null;
  status: number; // 1=ACTIVE 0=ARCHIVED
  createTime?: string;
  updateTime?: string;
}

/** AI 对话附件 */
export interface ChatAttachmentDTO {
  url: string;
  name?: string;
  contentType?: string;
  size?: number;
}

/** AI 会话消息（一问一答） */
export interface ConversationMessageDTO {
  id: number;
  sessionId: string;
  question: string;
  answer: string;
  attachments?: ChatAttachmentDTO[];
  requestId?: string;
  finishReason?: string;
  inputTokens?: number;
  outputTokens?: number;
  createTime?: string;
}

/** 热门问题 */
export interface HotQuestionDTO {
  question: string;
  hitCount?: number;
}

/** 创建会话分享结果 */
export interface ConversationShareCreateDTO {
  shareToken: string;
  shareUrl: string;
  expireAt?: string;
  title?: string;
}

/** 分享快照中的附件（脱敏，无 url） */
export interface ShareAttachmentDTO {
  name?: string;
  contentType?: string;
}

/** 分享快照中的一轮问答 */
export interface ShareMessageDTO {
  question: string;
  answer: string;
  createTime?: string;
  attachments?: ShareAttachmentDTO[];
}

/** 打开分享页内容 */
export interface ConversationShareViewDTO {
  title: string;
  expireAt?: string;
  messages: ShareMessageDTO[];
}

/** 文件文本抽取结果 */
export interface FileExtractResultDTO {
  fileName: string;
  contentType?: string;
  charCount: number;
  truncated: boolean;
  text: string;
}

export interface WechatQrConfigDTO {
  appId: string;
  redirectUri: string;
  scope: string;
  state: string;
  qrConnectUrl: string;
}

export interface UserSessionDTO {
  sessionId: string;
  deviceType: string;
  deviceName: string;
  clientIp: string;
  loginTime: string;
  lastActiveTime: string;
  current: boolean;
}
