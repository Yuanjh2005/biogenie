"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/lib/api/admin";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AdminConfirmCancelledError, useAdminConfirm } from "@/components/settings/admin-confirm-dialog";
import { useLocaleStore } from "@/stores/locale";
import { ADMIN_SETTINGS_COPY } from "@/lib/i18n/admin-settings";

function save(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url; link.download = fileName; link.click();
  URL.revokeObjectURL(url);
}

export function LogsPanel() {
  const locale = useLocaleStore((s) => s.locale);
  const copy = ADMIN_SETTINGS_COPY[locale];
  const [exporting, setExporting] = useState(false);
  const [behaviorPage, setBehaviorPage] = useState(1);
  const [auditPage, setAuditPage] = useState(1);
  const requestAdminConfirm = useAdminConfirm();
  const audit = useQuery({
    queryKey: ["admin-audit-logs", auditPage],
    queryFn: () => adminApi.auditLogs(auditPage, 20),
  });
  const behavior = useQuery({
    queryKey: ["api-behavior-logs", behaviorPage],
    queryFn: () => adminApi.behaviorLogs(behaviorPage, 20),
  });

  const exportLogs = async (type: "audit" | "behavior") => {
    setExporting(true);
    try {
      const token = await requestAdminConfirm();
      const result = type === "audit" ? await adminApi.exportAuditLogs(token) : await adminApi.exportBehaviorLogs(token);
      save(result.blob, type === "audit" ? "admin-audit.xlsx" : "api-behavior.xlsx");
      toast.success(copy.exportSuccess);
    } catch (error) { if (!(error instanceof AdminConfirmCancelledError)) toast.error(copy.exportFailed); } finally { setExporting(false); }
  };

  return <Tabs defaultValue="behavior" className="space-y-3">
    <TabsList><TabsTrigger value="behavior">{copy.behaviorTab}</TabsTrigger><TabsTrigger value="audit">{copy.auditTab}</TabsTrigger></TabsList>
    <TabsContent value="behavior" className="space-y-3">
      <Header title={copy.behaviorTitle} exportLabel={copy.exportExcel} disabled={exporting} onExport={() => exportLogs("behavior")} />
      <div className="max-h-[470px] w-full overflow-auto rounded-lg border">
        <table className="w-full table-fixed text-xs"><thead className="sticky top-0 z-10 bg-muted"><tr><th className="w-[22%] p-2 text-left">{copy.userRole}</th><th className="w-[38%] p-2 text-left">{copy.endpoint}</th><th className="w-[18%] p-2 text-left">{copy.statusDuration}</th><th className="w-[22%] p-2 text-left">{copy.time}</th></tr></thead>
          <tbody>{behavior.data?.records.map((row) => <tr key={row.id} className="border-t"><td className="break-words p-2">#{row.userId} · {row.userRole}</td><td className="break-all p-2 font-mono" title={`${row.requestMethod} ${row.requestPath}`}>{row.requestMethod} {row.requestPath}</td><td className="p-2">{row.statusCode} · {row.durationMs}ms</td><td className="p-2">{new Date(row.createTime).toLocaleString(locale)}</td></tr>)}</tbody>
        </table>
      </div>
      <Pagination current={behaviorPage} total={behavior.data?.total ?? 0} loading={behavior.isLoading} copy={copy} onChange={setBehaviorPage} />
    </TabsContent>
    <TabsContent value="audit" className="space-y-3">
      <Header title={copy.auditTitle} exportLabel={copy.exportExcel} disabled={exporting} onExport={() => exportLogs("audit")} />
      <div className="max-h-[470px] w-full overflow-auto rounded-lg border">
        <table className="w-full table-fixed text-xs"><thead className="sticky top-0 z-10 bg-muted"><tr><th className="w-[18%] p-2 text-left">{copy.operator}</th><th className="w-[32%] p-2 text-left">{copy.action}</th><th className="w-[28%] p-2 text-left">{copy.target}</th><th className="w-[22%] p-2 text-left">{copy.time}</th></tr></thead>
          <tbody>{audit.data?.records.map((row) => <tr key={row.id} className="border-t"><td className="p-2">#{row.operatorId}</td><td className="break-all p-2">{row.action}</td><td className="break-all p-2">{row.targetType} · {row.targetId}</td><td className="p-2">{new Date(row.createTime).toLocaleString(locale)}</td></tr>)}</tbody>
        </table>
      </div>
      <Pagination current={auditPage} total={audit.data?.total ?? 0} loading={audit.isLoading} copy={copy} onChange={setAuditPage} />
    </TabsContent>
  </Tabs>;
}

function Header({ title, exportLabel, disabled, onExport }: { title: string; exportLabel: string; disabled: boolean; onExport: () => void }) {
  return <div className="flex items-center justify-between"><h3 className="text-sm font-medium">{title}</h3><button type="button" disabled={disabled} onClick={onExport} className="inline-flex items-center gap-1 rounded-md border px-2.5 py-1.5 text-xs disabled:opacity-50"><Download className="size-3.5" />{exportLabel}</button></div>;
}

function Pagination({ current, total, loading, copy, onChange }: { current: number; total: number; loading: boolean; copy: (typeof ADMIN_SETTINGS_COPY)[keyof typeof ADMIN_SETTINGS_COPY]; onChange: (page: number) => void }) {
  const totalPages = Math.max(1, Math.ceil(total / 20));
  return <div className="flex items-center justify-between text-xs text-muted-foreground">
    <span>{copy.pagination(total)}</span>
    <div className="flex items-center gap-2">
      <button type="button" className="rounded-md border px-2.5 py-1.5 text-foreground disabled:cursor-not-allowed disabled:opacity-40" disabled={current <= 1 || loading} onClick={() => onChange(current - 1)}>{copy.previous}</button>
      <span>{current} / {totalPages}</span>
      <button type="button" className="rounded-md border px-2.5 py-1.5 text-foreground disabled:cursor-not-allowed disabled:opacity-40" disabled={current >= totalPages || loading} onClick={() => onChange(current + 1)}>{copy.next}</button>
    </div>
  </div>;
}
