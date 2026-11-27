/**
 * 解析 fetch 返回的 SSE 流（text/event-stream）。
 * 支持 `event:` 名 + `data:` 载荷；事件以空行分隔。
 */
export async function parseSSE(
  body: ReadableStream<Uint8Array>,
  onEvent: (data: string, eventName?: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const consume = (flush: boolean) => {
    buffer = buffer.replace(/\r\n/g, "\n").replace(/\r/g, "\n");
    let idx: number;
    while ((idx = buffer.indexOf("\n\n")) !== -1) {
      const rawEvent = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);
      emitEvent(rawEvent, onEvent);
    }
    if (flush && buffer.trim()) {
      emitEvent(buffer, onEvent);
      buffer = "";
    }
  };

  try {
    while (true) {
      if (signal?.aborted) break;
      const { done, value } = await reader.read();
      if (done) {
        buffer += decoder.decode();
        consume(true);
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      consume(false);
    }
  } finally {
    reader.releaseLock();
  }
}

function emitEvent(rawEvent: string, onEvent: (data: string, eventName?: string) => void) {
  let eventName: string | undefined;
  const dataLines: string[] = [];
  for (const line of rawEvent.split("\n")) {
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).replace(/^ /, ""));
    }
  }
  if (dataLines.length > 0) {
    onEvent(dataLines.join("\n"), eventName);
  }
}
