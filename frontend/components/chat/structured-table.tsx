"use client";

import type { StructuredTable } from "@/types/chat";
import { CopyTableButton, copyStructuredTable } from "@/components/chat/copy-table-button";

/** DesignMCP 结构化表格（维度 / 支持证据） */
export function StructuredTableView({ table }: { table?: StructuredTable }) {
  if (!table || table.rows.length === 0) return null;

  return (
    <div className="relative mt-3 overflow-x-auto rounded-lg border">
      <div className="flex justify-end border-b bg-muted/40 px-2 py-1">
        <CopyTableButton onCopy={() => copyStructuredTable(table.columns, table.rows)} />
      </div>
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr>
            {table.columns.map((c) => (
              <th key={c} className="border-b bg-muted px-3 py-2 text-left font-medium">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {table.rows.map((row, ri) => (
            <tr key={ri}>
              {row.map((cell, ci) => (
                <td key={ci} className="border-b px-3 py-2 align-top">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
