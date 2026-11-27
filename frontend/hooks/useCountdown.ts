"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** 倒计时（秒）。start() 从 seconds 开始递减到 0。 */
export function useCountdown(seconds = 60) {
  const [remaining, setRemaining] = useState(0);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);

  const clear = useCallback(() => {
    if (timer.current) {
      clearInterval(timer.current);
      timer.current = null;
    }
  }, []);

  const start = useCallback(() => {
    clear();
    setRemaining(seconds);
    timer.current = setInterval(() => {
      setRemaining((r) => {
        if (r <= 1) {
          clear();
          return 0;
        }
        return r - 1;
      });
    }, 1000);
  }, [clear, seconds]);

  useEffect(() => clear, [clear]);

  return { remaining, active: remaining > 0, start };
}
