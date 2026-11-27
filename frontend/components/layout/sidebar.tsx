"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { CirclePlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { MODULE_LIST, moduleFromPath, routeForModule } from "@/lib/config/agents";
import { ConversationList } from "@/components/layout/conversation-list";
import { UserMenu } from "@/components/layout/user-menu";
import {
  DeepPubMedIcon,
  DesignMcpIcon,
  DrgfIcon,
  HomeLineIcon,
  SmartDrgfIcon,
} from "@/components/layout/nav-icons";
import { useChatStore } from "@/stores/chat";
import { useLocaleStore } from "@/stores/locale";
import type { ModuleKey } from "@/types/chat";
import { SHELL_COPY } from "@/lib/i18n/shell";

const DRGF_HOME_URL = process.env.NEXT_PUBLIC_DRGF_HOME_URL || "/";

const NAV_ICONS: Record<ModuleKey, React.ComponentType<React.SVGProps<SVGSVGElement>>> = {
  "smart-drgf": SmartDrgfIcon,
  "deep-pubmed": DeepPubMedIcon,
  "design-mcp": DesignMcpIcon,
};

export function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const router = useRouter();
  const locale = useLocaleStore((s) => s.locale);
  const copy = SHELL_COPY[locale];
  const newConversation = useChatStore((s) => s.newConversation);

  // 首页不显示「新对话」；DRGF 复用 SmartDRGF
  const currentModule = pathname.startsWith("/home")
    ? undefined
    : moduleFromPath(pathname);

  const handleNewChat = () => {
    if (!currentModule) return;
    newConversation(currentModule);
    router.push(routeForModule(currentModule, pathname));
    onNavigate?.();
  };

  /** 首页始终进入空白欢迎态；已有对话保留在历史列表 */
  const handleHomeClick = () => {
    newConversation("smart-drgf");
    router.push("/home");
    onNavigate?.();
  };

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden bg-sidebar text-sidebar-foreground">
      {/* Logo */}
      <div className="flex items-center px-4 py-4">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="/images/login/logo.svg" alt="BioGenie" className="h-8 w-auto" />
      </div>

      {/* 新对话：仅在 SmartDRGF/DeepPubMed/DesignMCP 模块页显示 */}
      {currentModule && (
        <div className="px-3 pb-2">
          <Button
            className="w-full rounded-[12px] text-white shadow-[0_1px_2px_0_rgba(0,0,0,0.10)] hover:opacity-90"
            style={{ background: "linear-gradient(90deg, #4841F1 0%, #5D8BFF 100%)" }}
            onClick={handleNewChat}
          >
            <CirclePlus className="size-4" /> {copy.newChat}
          </Button>
        </div>
      )}

      {/* 导航 */}
      <nav className="space-y-0.5 px-3 py-2">
        <NavItem
          href="/home"
          active={pathname === "/home"}
          icon={<HomeLineIcon className="size-4" />}
          label={copy.home}
          onClick={handleHomeClick}
        />
        <NavItem
          href={DRGF_HOME_URL}
          active={false}
          icon={<DrgfIcon className="size-4" />}
          label="DRGF"
          external
          onNavigate={onNavigate}
        />
        {MODULE_LIST.map((m) => {
          const Icon = NAV_ICONS[m.key];
          return (
            <NavItem
              key={m.key}
              href={m.route}
              active={pathname.startsWith(m.route)}
              icon={<Icon className="size-4" />}
              label={m.label}
              onNavigate={onNavigate}
            />
          );
        })}
      </nav>

      <div className="mx-3 my-2 border-t" />

      <ConversationList onNavigate={onNavigate} />

      <UserMenu />
    </div>
  );
}

function NavItem({
  href,
  active,
  icon,
  label,
  onNavigate,
  onClick,
  external,
}: {
  href: string;
  active: boolean;
  icon: React.ReactNode;
  label: string;
  onNavigate?: () => void;
  /** 自定义点击（如首页需先清空当前会话） */
  onClick?: () => void;
  external?: boolean;
}) {
  const className = cn(
    "flex items-center gap-2 rounded-md px-2 py-2 text-sm hover:bg-accent",
    active && "bg-[#EFF3FD] font-medium text-[#4C4FF4] hover:bg-[#EFF3FD]",
  );
  const content = (
    <>
      {icon}
      <span>{label}</span>
    </>
  );

  if (external) {
    return (
      <a href={href} className={className} onClick={() => onNavigate?.()}>
        {content}
      </a>
    );
  }

  return (
    <Link
      href={href}
      onClick={(e) => {
        if (onClick) {
          e.preventDefault();
          onClick();
          return;
        }
        onNavigate?.();
      }}
      className={className}
    >
      {content}
    </Link>
  );
}
