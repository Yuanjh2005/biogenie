"use client";

import { Suspense } from "react";
import { ChatPage } from "@/components/chat/chat-page";

export default function DrgfPage() {
  return (
    <Suspense fallback={null}>
      <ChatPage module="smart-drgf" allowAttachments />
    </Suspense>
  );
}
