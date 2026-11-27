import { api } from "./client";
import type {
  PageResult,
  UpdateUserDTO,
  UserInfoDTO,
  UserPageQuery,
  UserRecordDTO,
} from "@/types/api";

export const userApi = {
  /** 当前登录用户资料 */
  profile: () => api.get<UserInfoDTO>("/user/profile"),
  /** @deprecated 使用 profile */
  current: () => api.get<UserInfoDTO>("/user/profile"),

  update: (dto: UpdateUserDTO) => api.put<string>("/user/update", dto),

  updateAvatar: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api.put<string>("/user/avatar", undefined, { raw: form });
  },

  /** 分页查询用户列表（管理员，用户管理 Tab） */
  page: (params: UserPageQuery) =>
    api.get<PageResult<UserRecordDTO>>("/user/page", {
      query: {
        current: params.current,
        size: params.size,
        keyword: params.keyword,
      },
    }),

  /** 禁用/启用用户（管理员） */
  updateStatus: async (id: number, status: 0 | 1, confirmToken: string) => {
    return api.put<string>(`/user/status/${id}`, undefined, {
      query: { status },
      headers: { "X-Admin-Confirm-Token": confirmToken },
    });
  },

  createAdmin: async (phone: string, nickname: string | undefined, remark: string | undefined, confirmToken: string) => {
    return api.post<UserInfoDTO>("/user/admin", { phone, nickname, remark }, {
      headers: { "X-Admin-Confirm-Token": confirmToken },
    });
  },

  updateRole: async (id: number, role: "USER" | "ADMIN", remark: string | undefined, confirmToken: string) => {
    return api.put<UserInfoDTO>(`/user/role/${id}`, { role, remark }, {
      headers: { "X-Admin-Confirm-Token": confirmToken },
    });
  },
};
