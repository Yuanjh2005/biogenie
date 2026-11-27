/** 找到内部可滚动容器，忽略 window / body，避免把整页（侧栏+主栏）一起滚走。 */
export function getScrollParent(el: HTMLElement | null): HTMLElement | null {
  let node = el?.parentElement;
  while (node && node !== document.body && node !== document.documentElement) {
    const { overflowY } = getComputedStyle(node);
    if (overflowY === "auto" || overflowY === "scroll") return node;
    node = node.parentElement;
  }
  return null;
}

/** 只调整内部 scroller 的 scrollTop，不调用会带动视口的 scrollIntoView。 */
export function scrollChildIntoContainer(
  el: HTMLElement,
  options?: { offset?: number },
): void {
  const scroller = getScrollParent(el);
  if (!scroller) return;
  const elRect = el.getBoundingClientRect();
  const scrollerRect = scroller.getBoundingClientRect();
  scroller.scrollTop += elRect.top - scrollerRect.top - (options?.offset ?? 0);
  window.scrollTo(0, 0);
}
