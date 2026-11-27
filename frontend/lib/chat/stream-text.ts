/**
 * 合并流式文本：同时兼容「增量 delta」和「到目前为止的全文快照」。
 *
 * 百炼首轮 / 思考过程经常每帧返回累计全文。前端若一律 `+=`，
 * 会叠成「你你好你好世…」这类看起来像乱码的重复文本。
 */
export function appendStreamDelta(prev: string, next: string | undefined): string {
  if (!next) return prev;
  if (!prev) return next;
  if (next === prev) return prev;
  if (next.startsWith(prev)) return next;
  if (prev.startsWith(next)) return prev;
  return prev + next;
}
