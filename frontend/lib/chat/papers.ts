import type { DocReference } from "@/types/chat";
import type { Locale } from "@/stores/locale";

const REF_HEADING =
  /(?:^|\n)[ \t]*(?:#{1,6}[ \t]*)?(?:参考文献|引用文献|文献列表|References)[ \t]*[:：]?\s*\n/i;

const ITEM_RE = /^(?:\[\^?(\d+)\]|(\d+)[\.、)])\s*:?[ \t]*(.+)$/gm;
const FOOTNOTE_BLOCK_RE = /(?:^|\n)[ \t]*\[\^\d+\][ \t]*:/m;
const PMID_RE = /PMID[:\s]*([0-9]{5,})/i;
const DOI_RE = /\b(10\.\d{4,9}\/[-._;()/:A-Z0-9]+)/i;
const URL_RE = /https?:\/\/[^\s)>\]]+/i;
const MD_LINK_RE = /\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/;
const HTML_LINK_RE = /<a\b[^>]*href=["'](https?:\/\/[^"']+)["'][^>]*>([\s\S]*?)<\/a>/i;
const EMPTY_REF_ANCHOR_RE = /<a\b[^>]*\bid=["']ref-?\d+["'][^>]*>\s*<\/a>/gi;

function pubmedUrlFromPmid(pmid: string): string {
  return `https://pubmed.ncbi.nlm.nih.gov/${pmid}/`;
}

function doiUrl(doi: string): string {
  return `https://doi.org/${doi.replace(/[.,;]+$/, "")}`;
}

function stripTags(raw: string): string {
  return raw.replace(/<[^>]+>/g, "");
}

function cleanTitle(raw: string): string {
  return stripTags(raw)
    .replace(MD_LINK_RE, "$1")
    .replace(URL_RE, "")
    .replace(PMID_RE, "")
    .replace(/doi:\s*/i, "")
    .replace(DOI_RE, "")
    .replace(/\*\*/g, "")
    .replace(/\s+/g, " ")
    .replace(/^[\-–—.\s]+/, "")
    .replace(/[.,\s]+$/, "")
    .trim();
}

function cleanCitation(raw: string): string {
  return stripTags(raw)
    .replace(MD_LINK_RE, "$1")
    .replace(URL_RE, "")
    .replace(PMID_RE, "PMID: $1")
    .replace(/\*\*/g, "")
    .replace(/\s+/g, " ")
    .replace(/^[\-–—.\s]+/, "")
    .replace(/[\s]+$/, "")
    .trim();
}

/** 文献卡片展示作者、标题、期刊、年份、PMID 等完整引文。 */
export function formatPaperCitation(paper: DocReference): string {
  const citation = cleanCitation(paper.citation || paper.title);
  const parts = [citation];
  const normalizedCitation = citation.toLowerCase();
  for (const value of [paper.journal, paper.year]) {
    const text = String(value ?? "").trim();
    if (text && !normalizedCitation.includes(text.toLowerCase())) parts.push(text);
  }
  return parts.filter(Boolean).join(", ");
}

export type CitationParts = {
  before: string;
  journal?: string;
  after?: string;
};

const YEAR_TAIL_RE = /,\s*((?:19|20)\d{2})\b(.*)$/;

/** 从完整引文中拆出期刊名，供卡片斜体展示。 */
export function splitPaperCitation(paper: DocReference): CitationParts {
  const full = formatPaperCitation(paper);
  if (!full) return { before: "" };

  const explicit = String(paper.journal ?? "").trim();
  const journal = explicit || extractJournalFromCitation(paper, full);
  if (!journal) return { before: full };

  const idx = full.toLowerCase().indexOf(journal.toLowerCase());
  if (idx < 0) return { before: full };

  return {
    before: full.slice(0, idx),
    journal: full.slice(idx, idx + journal.length),
    after: full.slice(idx + journal.length),
  };
}

function extractJournalFromCitation(paper: DocReference, full: string): string | undefined {
  const candidates = [paper.title, cleanTitle(paper.title || "")]
    .map((value) => String(value ?? "").trim())
    .filter((value, index, list) => value.length >= 8 && list.indexOf(value) === index);

  // title 往往是纯论文标题，citation 更完整：标题后、年份前即为期刊。
  for (const title of candidates) {
    if (title.length + 8 >= full.length) continue;
    const titleIdx = full.toLowerCase().indexOf(title.toLowerCase());
    if (titleIdx < 0) continue;
    const afterTitle = full.slice(titleIdx + title.length);
    const yearMatch = afterTitle.match(YEAR_TAIL_RE);
    if (!yearMatch || yearMatch.index == null) continue;
    const between = afterTitle.slice(0, yearMatch.index).replace(/^[\s,]+|[\s,]+$/g, "");
    if (between.length >= 3) return between;
  }

  // 无独立标题时：取紧挨年份、首词大写的片段（支持 "Artificial cells, nanomedicine, and biotechnology"）。
  const journalMatch = full.match(
    /,\s+([A-Z][^,]*(?:,\s+(?:and\s+)?[a-z][^,]*)*),\s*((?:19|20)\d{2})\b/,
  );
  return journalMatch?.[1]?.trim() || undefined;
}

function parseItem(text: string, index: number): DocReference | null {
  const pmid = text.match(PMID_RE)?.[1];
  const doi = text.match(DOI_RE)?.[1];
  const html = text.match(HTML_LINK_RE);
  const md = text.match(MD_LINK_RE);
  const url =
    html?.[1] ??
    md?.[2] ??
    text.match(URL_RE)?.[0]?.replace(/[.,;]+$/, "") ??
    (pmid ? pubmedUrlFromPmid(pmid) : undefined) ??
    (doi ? doiUrl(doi) : undefined);
  const title = cleanTitle(
    (html?.[2] && html[2].trim().length >= 8 ? html[2] : undefined) ??
      (md?.[1] && md[1].trim().length >= 8 ? md[1] : undefined) ??
      text,
  );
  if (!title || title.length < 8) return null;
  return { index, title, citation: cleanCitation(text), url };
}

function normalizeBibliography(section: string): string {
  return section
    .split("\n")
    .map((line) =>
      line
        .replace(EMPTY_REF_ANCHOR_RE, "")
        .replace(/^\s*\*\*/, "")
        .replace(/\*\*\s*$/, ""),
    )
    .join("\n");
}

/** 从回答文末参考文献列表中解析文献条目。 */
export function extractPapersFromContent(content: string): DocReference[] {
  if (!content.trim()) return [];

  const heading = content.search(REF_HEADING);
  if (heading >= 0) {
    const section = normalizeBibliography(content.slice(heading));
    const papers: DocReference[] = [];
    let match: RegExpExecArray | null;
    const re = new RegExp(ITEM_RE.source, "gm");
    while ((match = re.exec(section)) !== null) {
      const index = Number(match[1] ?? match[2]);
      const item = parseItem(match[3], index);
      if (item) papers.push(item);
    }
    if (papers.length > 0) return papers;
  }

  const fallback: DocReference[] = [];
  for (const line of content.split("\n")) {
    const numbered = line.match(new RegExp(ITEM_RE.source));
    if (numbered) {
      const source = numbered[3];
      if (MD_LINK_RE.test(source) || URL_RE.test(source) || PMID_RE.test(source) || DOI_RE.test(source)) {
        const item = parseItem(source, Number(numbered[1] ?? numbered[2]));
        if (item) fallback.push(item);
      }
      continue;
    }
    if (!PMID_RE.test(line) && !/pubmed\.ncbi\.nlm\.nih\.gov/i.test(line) && !DOI_RE.test(line)) continue;
    const item = parseItem(line.trim(), fallback.length + 1);
    if (item) fallback.push(item);
  }
  return fallback;
}

/** 有文献卡片时隐藏回答末尾的重复参考文献列表。 */
export function stripBibliographyFromContent(content: string): string {
  const heading = content.search(REF_HEADING);
  if (heading >= 0) return content.slice(0, heading).trimEnd();
  const footnotes = content.search(FOOTNOTE_BLOCK_RE);
  return footnotes >= 0 ? content.slice(0, footnotes).trimEnd() : content;
}

function paperKey(paper: DocReference): string {
  if (paper.url) return paper.url.toLowerCase().replace(/\/+$/, "");
  return paper.title.toLowerCase().replace(/\s+/g, " ").trim();
}

function moreCompleteTitle(first: string, second: string): string {
  const cleanFirst = cleanTitle(first);
  const cleanSecond = cleanTitle(second);
  return cleanSecond.length > cleanFirst.length ? cleanSecond : cleanFirst;
}

function moreCompleteCitation(first: DocReference, second: DocReference): string {
  const cleanFirst = cleanCitation(first.citation || first.title);
  const cleanSecond = cleanCitation(second.citation || second.title);
  return cleanSecond.length > cleanFirst.length ? cleanSecond : cleanFirst;
}

export function mergePapers(
  structured?: DocReference[],
  extracted?: DocReference[],
): DocReference[] {
  const merged: DocReference[] = [];
  for (const paper of [...(extracted ?? []), ...(structured ?? [])]) {
    const key = paperKey(paper);
    if (!key) continue;
    const found = merged.findIndex(
      (candidate) =>
        paperKey(candidate) === key ||
        (candidate.index != null && paper.index != null && candidate.index === paper.index),
    );
    if (found < 0) {
      merged.push({ ...paper, title: cleanTitle(paper.title), citation: cleanCitation(paper.citation || paper.title) });
      continue;
    }
    const prev = merged[found];
    merged[found] = {
      ...prev,
      ...paper,
      // URL 不同但编号相同的结构化短标题和正文完整引文仍属于同一篇文献。
      title: moreCompleteTitle(prev.title, paper.title),
      citation: moreCompleteCitation(prev, paper),
      url: paper.url || prev.url,
    };
  }
  return merged.map((p, i) => ({ ...p, index: p.index ?? i + 1 }));
}

export function pubmedSearchUrl(title: string): string {
  return `https://pubmed.ncbi.nlm.nih.gov/?term=${encodeURIComponent(title)}`;
}

export function mindMapPrompt(paper: DocReference, locale: Locale = "zh-CN"): string {
  if (locale === "en") {
    const extra = paper.url ? `\nURL: ${paper.url}` : "";
    return (
      `Generate an AI mind-map overview of the following paper. Use a hierarchy covering background, methods, key findings, conclusions, and limitations so the full paper can be grasped quickly.\n\n` +
      `Paper: ${paper.title}${extra}`
    );
  }
  const extra = paper.url ? `\n原文链接：${paper.url}` : "";
  return (
    `请针对以下文献生成 AI 脑图速览，用层级结构梳理研究背景、方法、关键发现、结论与局限，便于快速把握全文。\n\n` +
    `文献：${paper.title}${extra}`
  );
}

export function dataInsightPrompt(paper: DocReference, locale: Locale = "zh-CN"): string {
  if (locale === "en") {
    const extra = paper.url ? `\nURL: ${paper.url}` : "";
    return (
      `Interpret the key data in the following paper (sample size, primary endpoints, statistical significance, effect sizes, and figure/table meaning) with evidence-based commentary. Do not write only a couple of empty sentences.\n\n` +
      `Paper: ${paper.title}${extra}`
    );
  }
  const extra = paper.url ? `\n原文链接：${paper.url}` : "";
  return (
    `请深入解读以下文献中的关键数据（样本量、主要终点、统计显著性、效应量、图表含义），给出有依据的解读，不要只写一两句空话。\n\n` +
    `文献：${paper.title}${extra}`
  );
}
