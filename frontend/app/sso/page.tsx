import { Suspense } from "react";
import { SsoLoading, SsoLoginHandler } from "@/components/auth/sso-login-handler";

export default function SsoPage() {
  return (
    <Suspense fallback={<SsoLoading />}>
      <SsoLoginHandler />
    </Suspense>
  );
}
