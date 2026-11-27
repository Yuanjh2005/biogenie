import { api } from "./client";
import type { FileExtractResultDTO } from "@/types/api";

export const fileApi = {
  /** 上传单文件，返回文件 URL */
  upload: (file: File, prefix: string, signal?: AbortSignal) => {
    const form = new FormData();
    form.append("file", file);
    return api.post<string>("/file/upload", undefined, {
      raw: form,
      query: { prefix },
      signal,
    });
  },

  uploadImage: (file: File, prefix: string) => {
    const form = new FormData();
    form.append("file", file);
    return api.post<string>("/file/upload/image", undefined, {
      raw: form,
      query: { prefix },
    });
  },

  /** 抽取文档纯文本（pdf/doc/docx/xls/xlsx/txt/md，≤10MB） */
  extract: (file: File, signal?: AbortSignal) => {
    const form = new FormData();
    form.append("file", file);
    return api.post<FileExtractResultDTO>("/file/extract", undefined, {
      raw: form,
      signal,
    });
  },

  remove: (fileUrl: string) =>
    api.delete<string>("/file/delete", { query: { fileUrl } }),
};
