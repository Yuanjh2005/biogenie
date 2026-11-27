"use client";

import { Suspense } from "react";
import { ChatPage } from "@/components/chat/chat-page";

export default function DeepPubmedPage() {
  return (
    <Suspense fallback={null}>
      <ChatPage module="deep-pubmed" allowAttachments />
    </Suspense>
  );
}
