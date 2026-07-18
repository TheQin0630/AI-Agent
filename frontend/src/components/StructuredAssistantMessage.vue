<template>
  <div class="structured-reply">
    <section
      v-for="(section, sectionIndex) in sections"
      :key="`${section.title}-${sectionIndex}`"
      class="reply-section"
      :class="section.tone"
    >
      <header v-if="section.title">
        <span><component :is="sectionIcon(section.tone)" :size="14" /></span>
        <strong>{{ section.title }}</strong>
      </header>
      <template v-for="(block, blockIndex) in section.blocks" :key="blockIndex">
        <p v-if="block.type === 'text'" class="reply-text">
          <b v-if="block.label">{{ block.label }}</b
          >{{ block.text }}
        </p>
        <div
          v-else-if="block.type === 'risk'"
          class="risk-block"
          :class="`risk-${block.level?.toLowerCase()}`"
        >
          <span>{{ block.level }}</span>
          <p>{{ block.text }}</p>
        </div>
        <div v-else-if="block.type === 'table'" class="reply-table-wrap">
          <table>
            <thead v-if="block.headers?.length">
              <tr>
                <th v-for="header in block.headers" :key="header">
                  {{ header }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
                <td v-for="(cell, cellIndex) in row" :key="cellIndex">
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from "vue";
import {
  CircleAlert,
  FileSearch2,
  Lightbulb,
  MessageSquareText,
} from "lucide-vue-next";

type Tone = "intro" | "core" | "risk" | "advice";
type TextBlock = { type: "text"; text: string; label?: string };
type RiskBlock = { type: "risk"; text: string; level: string };
type TableBlock = { type: "table"; headers: string[]; rows: string[][] };
type Block = TextBlock | RiskBlock | TableBlock;
type Section = { title: string; tone: Tone; blocks: Block[] };
const props = defineProps<{ content?: string }>();

const titleMap: Record<Tone, string> = {
  intro: "分析结论",
  core: "核心差异",
  risk: "风险提示",
  advice: "处理建议",
};
function sectionIcon(tone: Tone): Component {
  return (
    {
      intro: MessageSquareText,
      core: FileSearch2,
      risk: CircleAlert,
      advice: Lightbulb,
    } as Record<Tone, Component>
  )[tone];
}
function cleanInline(value: string) {
  return value
    .replace(/<[^>]+>/g, " ")
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .replace(/[*_`]/g, "")
    .trim();
}
function toneForHeading(value: string): Tone | null {
  if (/核心差异|差异对比/.test(value)) return "core";
  if (/风险提示|风险分析/.test(value)) return "risk";
  if (/处理建议|建议|下一步/.test(value)) return "advice";
  return null;
}
function parseReply(source = "") {
  const result: Section[] = [];
  const ensure = (tone: Tone) => {
    let section = result.find((item) => item.tone === tone);
    if (!section) {
      section = { title: titleMap[tone], tone, blocks: [] };
      result.push(section);
    }
    return section;
  };
  let current = ensure("intro");
  const lines = source.split(/\r?\n/);
  for (let index = 0; index < lines.length; index += 1) {
    const raw = lines[index].trim();
    if (!raw || /^-{3,}$/.test(raw)) continue;
    const heading = cleanInline(raw.replace(/^#{1,6}\s*/, ""));
    const nextTone = toneForHeading(heading);
    if (
      nextTone &&
      (/^#{1,6}/.test(raw) ||
        /^(核心差异|差异对比|风险提示|风险分析|处理建议|建议|下一步)/.test(
          heading,
        ))
    ) {
      current = ensure(nextTone);
      continue;
    }
    if (raw.startsWith("|")) {
      const tableLines: string[] = [];
      while (index < lines.length && lines[index].trim().startsWith("|"))
        tableLines.push(lines[index++].trim());
      index -= 1;
      const rows = tableLines
        .map((line) => line.split("|").slice(1, -1).map(cleanInline))
        .filter((row) => !row.every((cell) => /^:?-{3,}:?$/.test(cell)));
      if (rows.length)
        current.blocks.push({
          type: "table",
          headers: rows[0],
          rows: rows.slice(1),
        });
      continue;
    }
    const cleaned = cleanInline(raw.replace(/^\s*(?:[-*+] |\d+[.)]\s*)/, ""));
    if (!cleaned) continue;
    const risk = cleaned.match(
      /^[🔴🟠🟡🟢⚠️\s]*(HIGH|MEDIUM|LOW)\b\s*[—–-]?\s*(.*)$/i,
    );
    if (risk) {
      current = ensure("risk");
      current.blocks.push({
        type: "risk",
        level: risk[1].toUpperCase(),
        text: risk[2] || cleaned,
      });
      continue;
    }
    const labelled = cleaned.match(/^([^：:]{2,18})[：:]\s*(.+)$/);
    current.blocks.push({
      type: "text",
      label: labelled ? `${labelled[1]}：` : undefined,
      text: labelled ? labelled[2] : cleaned,
    });
  }
  return result.filter((section) => section.blocks.length);
}
const sections = computed(() => parseReply(props.content));
</script>

<style scoped>
.structured-reply {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.reply-section {
  overflow: hidden;
  border: 0;
  outline: 0;
  border-radius: 12px;
  background: white;
  box-shadow: none;
}
.reply-section > header {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 12px;
  border-bottom: 0;
  background: #f7f9fc;
}
.reply-section > header span {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #e8f1ff;
  color: var(--primary);
}
.reply-section > header strong {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}
.reply-section.risk > header {
  background: #fff9f1;
}
.reply-section.risk > header span {
  background: #fff0dc;
  color: #bd6b00;
}
.reply-section.advice > header {
  background: #f3fbf7;
}
.reply-section.advice > header span {
  background: #e3f7ee;
  color: #087a55;
}
.reply-text {
  margin: 0;
  padding: 10px 12px;
  color: #344054;
  font-size: 14px;
  line-height: 1.72;
}
.reply-text + .reply-text {
  padding-top: 0;
}
.reply-text b {
  color: #1f2a3a;
}
.risk-block {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 10px 12px;
  padding: 10px 12px;
  border-radius: 9px;
  background: #f7f9fc;
}
.risk-block > span {
  padding: 3px 7px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 900;
}
.risk-block p {
  margin: 0;
  color: #344054;
  font-size: 14px;
  line-height: 1.7;
}
.risk-high {
  background: #fff4f5;
}
.risk-high > span {
  background: #ffe0e3;
  color: #c82f40;
}
.risk-medium {
  background: #fff9ef;
}
.risk-medium > span {
  background: #ffebc8;
  color: #a96100;
}
.risk-low {
  background: #f1fbf6;
}
.risk-low > span {
  background: #dcf5e8;
  color: #087a55;
}
.reply-table-wrap {
  margin: 10px 12px 12px;
  overflow-x: auto;
  border: 0;
  border-radius: 9px;
  background: #f8fafc;
}
.reply-table-wrap table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.reply-table-wrap th,
.reply-table-wrap td {
  min-width: 90px;
  padding: 9px 10px;
  border-bottom: 1px solid #edf1f6;
  text-align: left;
  vertical-align: top;
}
.reply-table-wrap th {
  background: #f6f8fb;
  color: #607087;
  font-weight: 700;
}
.reply-table-wrap tr:last-child td {
  border-bottom: 0;
}
</style>
