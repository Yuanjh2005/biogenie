"use client";

import { Suspense } from "react";
import { ChatPage } from "@/components/chat/chat-page";

export default function DesignMcpPage() {
  return (
    <Suspense fallback={null}>
      <ChatPage module="design-mcp" allowAttachments />
    </Suspense>
  );
}
