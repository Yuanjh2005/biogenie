import type { Locale } from "@/stores/locale";

export type AgreementType = "user" | "privacy";

export const AGREEMENT_TITLES: Record<Locale, Record<AgreementType, string>> = {
  "zh-CN": {
    user: "BioGenie 用户服务协议",
    privacy: "BioGenie 隐私政策",
  },
  en: {
    user: "BioGenie Terms of Service",
    privacy: "BioGenie Privacy Policy",
  },
};

export const AGREEMENT_CONTENT: Record<Locale, Record<AgreementType, string>> = {
  "zh-CN": {
    user: `生效日期：2026 年 1 月 1 日

欢迎使用 BioGenie（以下简称「本平台」）。请您在注册、登录或使用本平台前仔细阅读本协议。您点击登录、完成注册或实际使用本平台，即视为已阅读并同意本协议。

一、服务说明
1. 本平台面向科研与产业用户，提供细胞生长因子等相关领域的智能问答、文献检索与解读、蛋白药物设计辅助等能力。
2. AI 生成内容仅供科研参考，不构成医疗诊断、用药建议、监管申报结论或任何专业意见。您应结合自身专业知识进行独立判断。
3. 因模型能力、数据时效或输入信息不足，输出可能存在不准确、不完整或滞后的情况，请勿将本平台结果作为唯一依据。

二、账号与注册
1. 您可通过手机号验证码或微信授权登录。未注册的手机号在首次验证通过后将自动完成注册。
2. 您应妥善保管账号、验证码及登录设备。因您自身原因导致的账号盗用或信息泄露，由您自行承担相应责任。
3. 您不得将账号出借、转让、出售给他人，不得利用本平台从事违法、侵权或干扰服务正常运行的行为。

三、使用规范
1. 不得上传或输入违法、侵权、涉密或未经授权的第三方数据。
2. 不得对模型进行恶意探测、批量抓取、绕过安全限制或用于危害生物安全的用途。
3. 您对通过本平台提交的内容及基于输出所做的决策自行负责。

四、知识产权
1. 本平台的软件、界面、商标、文档及相关技术成果归平台运营方或其许可方所有。
2. 您在本平台生成的内容，在不侵犯第三方权利的前提下，由您按适用法律享有相应权利；平台可为提供、维护和优化服务之目的进行必要处理。

五、免责声明
1. 本平台按「现状」提供，不对特定研究结论、实验结果或商业目标作出保证。
2. 因不可抗力、网络故障、第三方服务中断或您违反本协议导致的损失，平台在法律允许范围内不承担责任。

六、协议变更与终止
1. 我们可能适时更新本协议，更新后将通过平台公示。继续使用即视为接受更新后的协议。
2. 如您违反本协议，我们有权暂停或终止向您提供服务。

如您对本协议有疑问，可通过平台内反馈渠道与我们联系。`,
    privacy: `生效日期：2026 年 1 月 1 日

BioGenie 重视您的个人信息与隐私保护。本政策说明我们如何收集、使用、存储和保护您的信息。

一、我们收集的信息
1. 账号信息：手机号、微信授权标识、昵称、头像等登录所需信息。
2. 使用信息：对话内容、上传文件、会话记录、操作日志，用于提供问答、检索、设计等服务及排查故障。
3. 设备与安全信息：登录时间、大致设备类型、必要的安全风控信息，用于保障账号安全。
4. 我们不会主动收集与服务无关的个人敏感信息。请勿在对话中提交他人隐私、病历原件中的可识别信息或无需提供的敏感数据。

二、我们如何使用信息
1. 创建与维护账号、完成身份核验与登录。
2. 提供智能对话、文献检索、文件解析与会话历史等核心功能。
3. 保障服务安全、防范滥用，并在法律要求时履行合规义务。
4. 在去标识化或聚合统计的前提下改进产品体验。未经您同意，我们不会将您的个人信息出售给第三方。

三、存储与保护
1. 我们在实现服务目的所需的最短期限内保存信息；超期后将删除或匿名化处理，法律法规另有规定的除外。
2. 我们采取合理的技术与管理措施保护数据，但互联网传输无法保证绝对安全。

四、共享、转让与公开
1. 为实现登录、短信、对象存储、大模型推理等功能，我们可能向受托处理方提供必要信息，并要求其仅按我们的指示处理。
2. 根据法律法规、监管要求或保护用户与公众权益之必要，我们可能披露相关信息。

五、您的权利
您可在平台「系统设置」中查看账号信息，申请变更绑定、退出登录或注销账号。注销后，我们将按法律法规停止提供服务并处理相关信息。

六、政策更新
本政策更新后将在平台公示。若您继续使用本平台，即表示您了解并接受更新后的政策。

如您对本政策有疑问，可通过平台内反馈渠道与我们联系。`,
  },
  en: {
    user: `Effective date: January 1, 2026

Welcome to BioGenie (the "Platform"). Please read these Terms before registering, signing in, or using the Platform. By signing in, completing registration, or using the Platform, you agree to these Terms.

1. Services
The Platform provides AI-assisted Q&A, literature search and interpretation, and protein-drug design support in growth factor and related fields for research and industry users. AI outputs are for scientific reference only and do not constitute medical, regulatory, or other professional advice. Results may be incomplete or outdated; do not rely on them as your sole basis for decisions.

2. Accounts
You may sign in with a phone verification code or WeChat authorization. An unregistered phone number is registered automatically after the first successful verification. Keep your account and devices secure. Do not transfer your account or use the Platform for unlawful, infringing, or abusive purposes.

3. Acceptable use
Do not upload unlawful, infringing, classified, or unauthorized third-party data. Do not probe, scrape, bypass safety controls, or use the Platform in ways that could harm biosafety. You are responsible for the content you submit and the decisions you make based on outputs.

4. Intellectual property
Software, UI, trademarks, and related technology belong to the operator or its licensors. Subject to applicable law and third-party rights, you retain rights in content you generate; we may process it as needed to provide, maintain, and improve the service.

5. Disclaimer
The Platform is provided "as is" and does not guarantee specific research conclusions or commercial outcomes. To the extent permitted by law, we are not liable for losses caused by force majeure, network failures, third-party outages, or your breach of these Terms.

6. Changes and termination
We may update these Terms and post them on the Platform. Continued use constitutes acceptance. We may suspend or terminate service if you violate these Terms.

Contact us through in-product feedback if you have questions.`,
    privacy: `Effective date: January 1, 2026

BioGenie respects your privacy. This Policy explains how we collect, use, store, and protect your information.

1. Information we collect
Account data (phone number, WeChat identifiers, nickname, avatar); usage data (conversations, uploads, session history, operation logs); and limited device/security data needed to protect your account. Please do not submit other people's personal data or unnecessary sensitive information in chats.

2. How we use information
We use it to create and maintain your account, provide chat/search/file features, protect against abuse, meet legal obligations, and improve the product using de-identified or aggregated data. We do not sell your personal information.

3. Storage and security
We retain information only as long as needed for the purposes above, then delete or anonymize it unless the law requires otherwise. We apply reasonable safeguards, but internet transmission is not perfectly secure.

4. Sharing
We may share necessary information with processors that provide SMS, storage, or model inference, who must follow our instructions. We may disclose information when required by law or to protect users and the public.

5. Your rights
You can review account details in Settings, change bindings, sign out, or delete your account. After deletion we will stop providing the service and handle related data as required by law.

6. Updates
We will post updates to this Policy on the Platform. Continued use means you understand and accept the updated Policy.

Contact us through in-product feedback if you have questions.`,
  },
};
