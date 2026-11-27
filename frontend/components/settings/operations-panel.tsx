"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApi } from "@/lib/api/admin";
import { Skeleton } from "@/components/ui/skeleton";
import { useLocaleStore } from "@/stores/locale";
import { ADMIN_SETTINGS_COPY } from "@/lib/i18n/admin-settings";

const metrics = [
  "totalUsers", "activeUsers", "newUsers", "admins",
  "totalConversations", "newConversations", "messages", "outputTokens",
] as const;

export function OperationsPanel() {
  const locale = useLocaleStore((s) => s.locale);
  const copy = ADMIN_SETTINGS_COPY[locale];
  const { data, isLoading, isError } = useQuery({
    queryKey: ["operations-overview", 30],
    queryFn: () => adminApi.overview(),
  });

  if (isLoading) return <div className="grid grid-cols-2 gap-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-20" />)}</div>;
  if (isError || !data) return <p className="py-8 text-center text-sm text-muted-foreground">{copy.overviewLoadFailed}</p>;

  return <div className="space-y-4">
    <div>
      <h3 className="text-sm font-medium">{copy.overviewTitle}</h3>
      <p className="mt-1 text-xs text-muted-foreground">{copy.overviewRange(data.days)}</p>
    </div>
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
      {metrics.map((key) => <div key={key} className="rounded-lg border p-3">
        <div className="text-xs text-muted-foreground">{copy.metrics[key]}</div>
        <div className="mt-1 text-xl font-semibold">{Number(data[key]).toLocaleString()}</div>
      </div>)}
    </div>
  </div>;
}
