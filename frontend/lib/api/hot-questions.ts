import { api } from "./client";
import type { HotQuestionDTO } from "@/types/api";

export const hotQuestionApi = {
  /** 热门问题 TopN；app 不传则查 global */
  list: (limit = 3, app?: string) =>
    api.get<HotQuestionDTO[]>("/ai/hot-questions", {
      query: { limit, app },
    }),
};
