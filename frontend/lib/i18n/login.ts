import type { Locale } from "@/stores/locale";

export const LOGIN_COPY: Record<
  Locale,
  {
    brandTitle: string;
    brandSubtitle: string;
    heading: string;
    welcome: string;
    language: string;
    zh: string;
    en: string;
    phone: string;
    phonePlaceholder: string;
    code: string;
    codePlaceholder: string;
    sendCode: string;
    sending: string;
    login: string;
    loggingIn: string;
    agreePrefix: string;
    terms: string;
    and: string;
    privacy: string;
    autoRegister: string;
    wechat: string;
    wechatScanTip: string;
    retry: string;
    featureInDevelopment: string;
    stayTuned: string;
    qrLoading: string;
    qrFailed: string;
    codeSent: string;
    sendCodeFailed: string;
    loginSuccess: string;
    loginFailed: string;
    wechatStateFailed: string;
    wechatLoginFailed: string;
    agreeEnd: string;
    passwordLogin: string;
    smsLogin: string;
    username: string;
    usernamePlaceholder: string;
    password: string;
    passwordPlaceholder: string;
    confirmPassword: string;
    confirmPasswordPlaceholder: string;
    passwordMismatch: string;
    usernameRequirement: string;
    passwordLengthRequirement: string;
    passwordCompositionRequirement: string;
    passwordsMatch: string;
    showPassword: string;
    hidePassword: string;
    noAccount: string;
    hasAccount: string;
    register: string;
    registering: string;
    registerSuccess: string;
  }
> = {
  "zh-CN": {
    brandTitle: "AI驱动的生物医学发现平台",
    brandSubtitle: "整合知识发现、证据挖掘与分子设计的多智能体系统",
    heading: "登录 BioGenie",
    welcome: "欢迎回来，请登录您的账户",
    language: "语言",
    zh: "简体中文",
    en: "English",
    phone: "手机号",
    phonePlaceholder: "请输入手机号",
    code: "验证码",
    codePlaceholder: "请输入6位验证码",
    sendCode: "发送验证码",
    sending: "发送中",
    login: "登录",
    loggingIn: "登录中…",
    agreePrefix: "点击登录即代表您同意我们的",
    terms: "服务协议",
    and: "和",
    privacy: "隐私政策",
    autoRegister: "未注册的手机号将自动注册",
    wechat: "微信扫码登录",
    wechatScanTip: "请使用微信扫描二维码登录",
    retry: "重新加载",
    featureInDevelopment: "功能开发中",
    stayTuned: "敬请期待",
    qrLoading: "加载中…",
    qrFailed: "二维码加载失败",
    codeSent: "验证码已发送",
    sendCodeFailed: "验证码发送失败",
    loginSuccess: "登录成功",
    loginFailed: "登录失败，请重试",
    wechatStateFailed: "登录校验失败，请重试",
    wechatLoginFailed: "微信登录失败",
    agreeEnd: "，",
    passwordLogin: "密码登录",
    smsLogin: "短信登录",
    username: "账号",
    usernamePlaceholder: "请输入4-32位账号",
    password: "密码",
    passwordPlaceholder: "至少8位，包含字母和数字",
    confirmPassword: "确认密码",
    confirmPasswordPlaceholder: "请再次输入密码",
    passwordMismatch: "两次输入的密码不一致",
    usernameRequirement: "账号需为 4-32 位字母、数字或 _ . -",
    passwordLengthRequirement: "密码长度为 8-72 位",
    passwordCompositionRequirement: "密码必须同时包含字母和数字",
    passwordsMatch: "两次输入的密码一致",
    showPassword: "显示密码",
    hidePassword: "隐藏密码",
    noAccount: "还没有账号？",
    hasAccount: "已有账号？",
    register: "立即注册",
    registering: "注册中…",
    registerSuccess: "注册成功",
  },
  en: {
    brandTitle: "AI Copilot for Biomedical Discovery",
    brandSubtitle:
      "A multi-agent system for knowledge discovery, evidence mining, and molecular design",
    heading: "Sign in to BioGenie",
    welcome: "Welcome back, please sign in to your account",
    language: "Language",
    zh: "简体中文",
    en: "English",
    phone: "Phone number",
    phonePlaceholder: "Enter your phone number",
    code: "Verification code",
    codePlaceholder: "Enter the 6-digit code",
    sendCode: "Send code",
    sending: "Sending",
    login: "Sign in",
    loggingIn: "Signing in…",
    agreePrefix: "By signing in, you agree to our",
    terms: "Terms of Service",
    and: "and",
    privacy: "Privacy Policy",
    autoRegister: "Unregistered phone numbers will be registered automatically",
    wechat: "WeChat QR sign-in",
    wechatScanTip: "Scan the QR code with WeChat to sign in",
    retry: "Try again",
    featureInDevelopment: "Feature in development",
    stayTuned: "Coming soon",
    qrLoading: "Loading…",
    qrFailed: "Failed to load QR code",
    codeSent: "Verification code sent",
    sendCodeFailed: "Failed to send the verification code",
    loginSuccess: "Signed in",
    loginFailed: "Sign-in failed. Please try again.",
    wechatStateFailed: "Sign-in verification failed. Please try again.",
    wechatLoginFailed: "WeChat sign-in failed",
    agreeEnd: ".",
    passwordLogin: "Password",
    smsLogin: "SMS code",
    username: "Username",
    usernamePlaceholder: "4–32 character username",
    password: "Password",
    passwordPlaceholder: "8+ chars with letters and numbers",
    confirmPassword: "Confirm password",
    confirmPasswordPlaceholder: "Enter your password again",
    passwordMismatch: "The passwords do not match",
    usernameRequirement: "Use 4-32 letters, numbers, or _ . -",
    passwordLengthRequirement: "Password must be 8-72 characters",
    passwordCompositionRequirement: "Password must contain letters and numbers",
    passwordsMatch: "The passwords match",
    showPassword: "Show password",
    hidePassword: "Hide password",
    noAccount: "No account?",
    hasAccount: "Already registered?",
    register: "Create account",
    registering: "Creating account…",
    registerSuccess: "Account created",
  },
};
