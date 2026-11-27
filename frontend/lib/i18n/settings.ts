import type { Locale } from "@/stores/locale";

export const SETTINGS_COPY: Record<
  Locale,
  {
    title: string;
    account: string;
    agreement: string;
    users: string;
    close: string;
    view: string;
    userAgreement: string;
    privacyPolicy: string;
    phone: string;
    change: string;
    wechat: string;
    bound: string;
    unbound: string;
    unbinding: string;
    unbind: string;
    wechatUnbound: string;
    unbindFailed: string;
    username: string;
    changeUsername: string;
    newUsername: string;
    newUsernamePlaceholder: string;
    usernameHint: string;
    usernameChanged: string;
    usernameChangeFailed: string;
    avatar: string;
    avatarHint: string;
    changeAvatar: string;
    avatarUploading: string;
    avatarUpdated: string;
    avatarUpdateFailed: string;
    avatarTypeError: string;
    avatarSizeError: string;
    logoutAll: string;
    loggingOut: string;
    logout: string;
    loggedOutAll: string;
    cancelAccount: string;
    cancel: string;
    confirmCancelTitle: string;
    confirmCancelBody: string;
    confirmCancel: string;
    cancelling: string;
    accountCancelled: string;
    cancelFailed: string;
    changePhone: string;
    newPhone: string;
    newPhonePlaceholder: string;
    code: string;
    codePlaceholder: string;
    sendCode: string;
    sending: string;
    submitting: string;
    confirmChange: string;
    codeSent: string;
    sendCodeFailed: string;
    phoneChanged: string;
    changeFailed: string;
    accountList: string;
    searchAccount: string;
    userAccount: string;
    status: string;
    actions: string;
    loadFailed: string;
    noMatches: string;
    enabled: string;
    disabled: string;
    disableAccount: string;
    restore: string;
    restored: string;
    accountDisabled: string;
    actionFailed: string;
  }
> = {
  "zh-CN": {
    title: "系统设置",
    account: "账号管理",
    agreement: "服务协议",
    users: "用户管理",
    close: "关闭",
    view: "查看",
    userAgreement: "用户协议",
    privacyPolicy: "隐私政策",
    phone: "手机号码",
    change: "变更",
    wechat: "微信",
    bound: "已绑定",
    unbound: "未绑定",
    unbinding: "解绑中…",
    unbind: "解绑",
    wechatUnbound: "微信已解绑",
    unbindFailed: "解绑失败，请重试",
    username: "用户名",
    changeUsername: "修改用户名",
    newUsername: "新用户名",
    newUsernamePlaceholder: "请输入新用户名",
    usernameHint: "用户名不能为空，最多 64 个字符",
    usernameChanged: "用户名修改成功",
    usernameChangeFailed: "用户名修改失败，请重试",
    avatar: "用户头像",
    avatarHint: "支持 JPG、PNG、GIF、WebP、BMP，最大 20MB",
    changeAvatar: "更换头像",
    avatarUploading: "上传中…",
    avatarUpdated: "头像更新成功",
    avatarUpdateFailed: "头像更新失败，请重试",
    avatarTypeError: "请选择 JPG、PNG、GIF、WebP 或 BMP 图片",
    avatarSizeError: "头像大小不能超过 20MB",
    logoutAll: "登出所有设备",
    loggingOut: "登出中…",
    logout: "登出",
    loggedOutAll: "已登出所有设备",
    cancelAccount: "注销账号",
    cancel: "取消",
    confirmCancelTitle: "确认注销账号？",
    confirmCancelBody: "注销后账号数据将无法恢复，且无法再使用当前手机号登录，请谨慎操作。",
    confirmCancel: "确认注销",
    cancelling: "注销中…",
    accountCancelled: "账号已注销",
    cancelFailed: "注销失败，请重试",
    changePhone: "变更手机号",
    newPhone: "新手机号",
    newPhonePlaceholder: "请输入新手机号",
    code: "验证码",
    codePlaceholder: "请输入6位验证码",
    sendCode: "发送验证码",
    sending: "发送中",
    submitting: "提交中…",
    confirmChange: "确认变更",
    codeSent: "验证码已发送",
    sendCodeFailed: "验证码发送失败",
    phoneChanged: "手机号变更成功",
    changeFailed: "变更失败，请重试",
    accountList: "账户列表",
    searchAccount: "搜索账号",
    userAccount: "用户账号",
    status: "状态",
    actions: "操作",
    loadFailed: "账户列表加载失败",
    noMatches: "暂无匹配账户",
    enabled: "启用中",
    disabled: "已禁用",
    disableAccount: "禁用账号",
    restore: "恢复启用",
    restored: "已恢复启用",
    accountDisabled: "已禁用该账户",
    actionFailed: "操作失败，请重试",
  },
  en: {
    title: "Settings",
    account: "Account",
    agreement: "Agreements",
    users: "Users",
    close: "Close",
    view: "View",
    userAgreement: "Terms of Service",
    privacyPolicy: "Privacy Policy",
    phone: "Phone number",
    change: "Change",
    wechat: "WeChat",
    bound: "Bound",
    unbound: "Not bound",
    unbinding: "Unbinding…",
    unbind: "Unbind",
    wechatUnbound: "WeChat unbound",
    unbindFailed: "Failed to unbind. Please try again.",
    username: "Username",
    changeUsername: "Change username",
    newUsername: "New username",
    newUsernamePlaceholder: "Enter a new username",
    usernameHint: "The username is required and can contain up to 64 characters",
    usernameChanged: "Username updated",
    usernameChangeFailed: "Failed to update the username. Please try again.",
    avatar: "Profile picture",
    avatarHint: "JPG, PNG, GIF, WebP or BMP, up to 20MB",
    changeAvatar: "Change picture",
    avatarUploading: "Uploading…",
    avatarUpdated: "Profile picture updated",
    avatarUpdateFailed: "Failed to update the profile picture. Please try again.",
    avatarTypeError: "Choose a JPG, PNG, GIF, WebP or BMP image",
    avatarSizeError: "The profile picture must be 20MB or smaller",
    logoutAll: "Sign out of all devices",
    loggingOut: "Signing out…",
    logout: "Sign out",
    loggedOutAll: "Signed out of all devices",
    cancelAccount: "Delete account",
    cancel: "Cancel",
    confirmCancelTitle: "Delete this account?",
    confirmCancelBody:
      "Deleted account data cannot be recovered, and this phone number can no longer be used to sign in. Please proceed with caution.",
    confirmCancel: "Delete account",
    cancelling: "Deleting…",
    accountCancelled: "Account deleted",
    cancelFailed: "Failed to delete the account. Please try again.",
    changePhone: "Change phone number",
    newPhone: "New phone number",
    newPhonePlaceholder: "Enter the new phone number",
    code: "Verification code",
    codePlaceholder: "Enter the 6-digit code",
    sendCode: "Send code",
    sending: "Sending",
    submitting: "Submitting…",
    confirmChange: "Confirm",
    codeSent: "Verification code sent",
    sendCodeFailed: "Failed to send the verification code",
    phoneChanged: "Phone number updated",
    changeFailed: "Failed to update. Please try again.",
    accountList: "Accounts",
    searchAccount: "Search accounts",
    userAccount: "Account",
    status: "Status",
    actions: "Actions",
    loadFailed: "Failed to load accounts",
    noMatches: "No matching accounts",
    enabled: "Active",
    disabled: "Disabled",
    disableAccount: "Disable",
    restore: "Enable",
    restored: "Account enabled",
    accountDisabled: "Account disabled",
    actionFailed: "Action failed. Please try again.",
  },
};
