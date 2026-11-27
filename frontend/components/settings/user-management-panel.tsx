"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { userApi } from "@/lib/api/user";
import { cn } from "@/lib/utils";
import { maskPhone } from "@/lib/validators";
import { SETTINGS_COPY } from "@/lib/i18n/settings";
import { useLocaleStore } from "@/stores/locale";
import { useAuthStore } from "@/stores/auth";
import type { UserRecordDTO } from "@/types/api";
import { AdminConfirmCancelledError, useAdminConfirm } from "@/components/settings/admin-confirm-dialog";
import { ADMIN_SETTINGS_COPY } from "@/lib/i18n/admin-settings";

const PAGE_SIZE = 10;

export function UserManagementPanel() {
  const queryClient = useQueryClient();
  const requestAdminConfirm = useAdminConfirm();
  const [keyword, setKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [adminPhone, setAdminPhone] = useState("");
  const [adminRemark, setAdminRemark] = useState("");
  const user = useAuthStore((s) => s.user);
  const isSuperAdmin = user?.role === "SUPER_ADMIN";
  const locale = useLocaleStore((s) => s.locale);
  const copy = SETTINGS_COPY[locale];
  const adminCopy = ADMIN_SETTINGS_COPY[locale];

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-users", keyword, currentPage],
    queryFn: () =>
      userApi.page({
        current: currentPage,
        size: PAGE_SIZE,
        keyword: keyword.trim() || undefined,
      }),
  });

  const records = data?.records ?? [];
  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / PAGE_SIZE));

  const toggleStatus = async (record: UserRecordDTO) => {
    const nextStatus = record.status === 1 ? 0 : 1;
    setPendingId(record.id);
    try {
      const confirmToken = await requestAdminConfirm();
      await userApi.updateStatus(record.id, nextStatus, confirmToken);
      toast.success(nextStatus === 1 ? copy.restored : copy.accountDisabled);
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
    } catch (error) {
      if (error instanceof AdminConfirmCancelledError) return;
      toast.error(copy.actionFailed);
    } finally {
      setPendingId(null);
    }
  };

  const createAdmin = async () => {
    if (!/^1[3-9]\d{9}$/.test(adminPhone)) return toast.error(adminCopy.invalidPhone);
    setPendingId(-1);
    try {
      const confirmToken = await requestAdminConfirm();
      await userApi.createAdmin(adminPhone, undefined, adminRemark || undefined, confirmToken);
      toast.success(adminCopy.administratorAdded);
      setAdminPhone(""); setAdminRemark("");
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
    } catch (error) { if (!(error instanceof AdminConfirmCancelledError)) toast.error(copy.actionFailed); } finally { setPendingId(null); }
  };

  const toggleRole = async (record: UserRecordDTO) => {
    const role = record.role === "ADMIN" ? "USER" : "ADMIN";
    setPendingId(record.id);
    try {
      const confirmToken = await requestAdminConfirm();
      await userApi.updateRole(record.id, role, role === "ADMIN" ? adminCopy.grantReason : adminCopy.revokeReason, confirmToken);
      toast.success(role === "ADMIN" ? adminCopy.administratorGranted : adminCopy.administratorRevoked);
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
    } catch (error) { if (!(error instanceof AdminConfirmCancelledError)) toast.error(copy.actionFailed); } finally { setPendingId(null); }
  };

  return (
    <div>
      {isSuperAdmin && <div className="mb-4 rounded-lg border p-3">
        <h3 className="text-sm font-medium">{adminCopy.addAdministrator}</h3>
        <div className="mt-2 flex gap-2">
          <Input value={adminPhone} onChange={(e) => setAdminPhone(e.target.value)} placeholder={adminCopy.phonePlaceholder} className="h-8" />
          <Input value={adminRemark} onChange={(e) => setAdminRemark(e.target.value)} placeholder={adminCopy.authorizationNote} className="h-8" />
          <button type="button" disabled={pendingId === -1} onClick={createAdmin} className="shrink-0 rounded-md bg-primary px-3 text-xs text-primary-foreground disabled:opacity-50">{adminCopy.add}</button>
        </div>
      </div>}
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="shrink-0 text-sm font-medium whitespace-nowrap">{copy.accountList}</h3>
        <div className="relative w-44 shrink-0">
          <Search className="absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setCurrentPage(1);
            }}
            placeholder={copy.searchAccount}
            className="h-8 pl-8 text-xs"
          />
        </div>
      </div>

      <div className="max-h-[445px] overflow-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="sticky top-0 z-10 bg-background">
            <tr className="border-b bg-muted/40 text-xs text-muted-foreground">
              <th className="px-3 py-2 text-left font-medium">{copy.userAccount}</th>
              <th className="px-3 py-2 text-left font-medium">{copy.status}</th>
              <th className="px-3 py-2 text-left font-medium">{adminCopy.roleAuthorization}</th>
              <th className="px-3 py-2 text-right font-medium">{copy.actions}</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <tr key={i} className="border-b last:border-0">
                    <td className="px-3 py-2.5" colSpan={4}>
                    <Skeleton className="h-4 w-full" />
                  </td>
                </tr>
              ))
            ) : isError ? (
              <tr>
                <td
                  className="px-3 py-6 text-center text-xs text-muted-foreground"
                  colSpan={4}
                >
                  {copy.loadFailed}
                </td>
              </tr>
            ) : records.length === 0 ? (
              <tr>
                <td
                  className="px-3 py-6 text-center text-xs text-muted-foreground"
                  colSpan={4}
                >
                  {copy.noMatches}
                </td>
              </tr>
            ) : (
              records.map((record) => {
                const enabled = record.status === 1;
                return (
                  <tr key={record.id} className="border-b last:border-0">
                    <td className="px-3 py-2.5">
                      {maskPhone(record.phone) || record.nickname || "-"}
                    </td>
                    <td className="px-3 py-2.5">
                      <span className="inline-flex items-center gap-1.5">
                        <span
                          className={cn(
                            "size-1.5 rounded-full",
                            enabled ? "bg-emerald-500" : "bg-destructive",
                          )}
                        />
                        {enabled ? copy.enabled : copy.disabled}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 text-xs">
                      <div>{record.role ?? "USER"}</div>
                      {record.adminGrantedAt && <div className="mt-0.5 text-[11px] text-muted-foreground" title={record.adminRemark}>
                        {adminCopy.grantedBy(record.adminGrantedByName || `#${record.adminGrantedBy}`)}
                      </div>}
                    </td>
                    <td className="px-3 py-2.5 text-right">
                      {isSuperAdmin && record.role !== "SUPER_ADMIN" && record.id !== user?.id && <button
                        type="button" className="mr-3 text-xs text-primary hover:underline disabled:opacity-50"
                        disabled={pendingId === record.id} onClick={() => toggleRole(record)}
                      >{record.role === "ADMIN" ? adminCopy.revokeAdministrator : adminCopy.makeAdministrator}</button>}
                      <button
                        type="button"
                        className={cn(
                          "text-xs hover:underline disabled:pointer-events-none disabled:opacity-50",
                          enabled ? "text-destructive" : "text-emerald-600",
                        )}
                        disabled={pendingId === record.id || record.id === user?.id || record.role === "SUPER_ADMIN"}
                        onClick={() => toggleStatus(record)}
                      >
                        {enabled ? copy.disableAccount : copy.restore}
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
        <span>{adminCopy.accountPagination(data?.total ?? 0, PAGE_SIZE)}</span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="rounded-md border px-2.5 py-1.5 text-foreground disabled:cursor-not-allowed disabled:opacity-40"
            disabled={currentPage <= 1 || isLoading}
            onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
          >
            {adminCopy.previous}
          </button>
          <span>{currentPage} / {totalPages}</span>
          <button
            type="button"
            className="rounded-md border px-2.5 py-1.5 text-foreground disabled:cursor-not-allowed disabled:opacity-40"
            disabled={currentPage >= totalPages || isLoading}
            onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
          >
            {adminCopy.next}
          </button>
        </div>
      </div>
    </div>
  );
}
