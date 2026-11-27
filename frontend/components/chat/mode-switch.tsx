"use client";

import { cn } from "@/lib/utils";
import { CHAT_COPY } from "@/lib/i18n/chat";
import { useLocaleStore } from "@/stores/locale";
import {
  ExpertFillIcon,
  ExpertLineIcon,
  FastFillIcon,
  FastLineIcon,
  WebFillIcon,
  WebLineIcon,
} from "@/components/chat/mode-icons";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ChatMode } from "@/types/chat";

type ModeIconComponent = React.ComponentType<React.SVGProps<SVGSVGElement>>;

const MODE_ICON: Record<ChatMode, { line: ModeIconComponent; fill: ModeIconComponent }> = {
  fast: { line: FastLineIcon, fill: FastFillIcon },
  expert: { line: ExpertLineIcon, fill: ExpertFillIcon },
  web: { line: WebLineIcon, fill: WebFillIcon },
};

interface ModeSwitchProps {
  modes: ChatMode[];
  value: ChatMode;
  onChange: (mode: ChatMode) => void;
  disabled?: boolean;
}

/** 欢迎页分段 Tab：一行均分、无分割线；选中态略高出外框并带背景阴影；首尾选中盖住外框圆弧 */
export function ModeSwitch({ modes, value, onChange, disabled }: ModeSwitchProps) {
  const locale = useLocaleStore((s) => s.locale);
  const labels = CHAT_COPY[locale].modes;
  return (
    <div
      className={cn(
        "mx-auto flex h-10 w-full items-center rounded-full border border-[#E5E7EB] bg-white",
        modes.length <= 2 ? "max-w-[280px]" : "max-w-[420px]",
      )}
    >
      {modes.map((m, index) => {
        const selected = value === m;
        const isFirst = index === 0;
        const isLast = index === modes.length - 1;
        const Icon = selected ? MODE_ICON[m].fill : MODE_ICON[m].line;
        return (
          <button
            key={m}
            type="button"
            disabled={disabled}
            onClick={() => onChange(m)}
            className={cn(
              "relative flex flex-1 items-center justify-center gap-1.5 whitespace-nowrap px-3 text-sm transition-all disabled:opacity-50",
              selected
                ? "z-10 -my-1 py-2.5 font-medium text-[#4D51F4]"
                : "py-2 text-[#0F172A] hover:text-[#4D51F4]/60",
              selected && "rounded-full",
              // 首尾选中向外扩，盖住外框左右圆弧与边线
              selected && isFirst && "-ml-[2px]",
              selected && isLast && "-mr-[2px]",
            )}
            style={
              selected
                ? {
                    border: "1px solid rgba(77, 81, 244, 0.40)",
                    background: "#EDF3FE",
                    boxShadow: "0 0 0 1px #B7C8FE, 0 4px 8px 0 rgba(0, 0, 0, 0.04)",
                  }
                : undefined
            }
          >
            <Icon className="size-3.5 shrink-0" />
            {labels[m]}
          </button>
        );
      })}
    </div>
  );
}

/** 对话中输入框右侧下拉切换 */
export function ModeSelect({ modes, value, onChange, disabled }: ModeSwitchProps) {
  const locale = useLocaleStore((s) => s.locale);
  const copy = CHAT_COPY[locale];
  const labels = copy.modes;
  const SelectedIcon = MODE_ICON[value]?.fill ?? MODE_ICON[value]?.line;

  return (
    <Select
      value={value}
      onValueChange={(v) => {
        if (v) onChange(v as ChatMode);
      }}
      disabled={disabled}
    >
      <SelectTrigger
        size="sm"
        className="h-8 gap-1.5 rounded-full border-[#E5E7EB] bg-white px-2.5 text-[#0F172A]"
        aria-label={copy.selectMode}
      >
        <SelectValue>
          <span className="inline-flex items-center gap-1.5">
            {SelectedIcon ? <SelectedIcon className="size-3.5 text-[#4D51F4]" /> : null}
            {labels[value]}
          </span>
        </SelectValue>
      </SelectTrigger>
      <SelectContent align="end" className="min-w-36">
        {modes.map((m) => {
          const Icon = MODE_ICON[m].line;
          return (
            <SelectItem key={m} value={m}>
              <span className="inline-flex items-center gap-1.5">
                <Icon className="size-3.5" />
                {labels[m]}
              </span>
            </SelectItem>
          );
        })}
      </SelectContent>
    </Select>
  );
}
