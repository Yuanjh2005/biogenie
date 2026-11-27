export function tableToTsv(columns: string[], rows: string[][]): string {
  const escape = (cell: string) => {
    if (/[\t\n"]/.test(cell)) return `"${cell.replace(/"/g, '""')}"`;
    return cell;
  };
  return [columns.map(escape).join("\t"), ...rows.map((row) => row.map(escape).join("\t"))].join(
    "\n",
  );
}

export function htmlTableToTsv(table: HTMLTableElement): string {
  const rows = Array.from(table.querySelectorAll("tr"));
  return rows
    .map((row) =>
      Array.from(row.querySelectorAll("th,td"))
        .map((cell) => {
          const text = (cell.textContent ?? "").replace(/\s+/g, " ").trim();
          if (/[\t\n"]/.test(text)) return `"${text.replace(/"/g, '""')}"`;
          return text;
        })
        .join("\t"),
    )
    .join("\n");
}

export { copyToClipboard as copyText } from "@/lib/clipboard";
