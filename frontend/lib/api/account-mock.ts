// 账号管理中暂无后端接口的操作（变更手机号 / 微信解绑 / 注销账号）。
// 后端接口就绪后，替换为 lib/api/user.ts 或新增 lib/api/auth.ts 中的真实调用。

function delay(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}

export const accountMock = {
  changePhone: async (_newPhone: string, _code: string) => {
    await delay(500);
    return { success: true };
  },

  unbindWechat: async () => {
    await delay(400);
    return { success: true };
  },

  cancelAccount: async () => {
    await delay(500);
    return { success: true };
  },
};
