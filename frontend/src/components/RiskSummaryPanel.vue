<template>
  <article class="risk-summary content-card">
    <header class="summary-heading">
      <div class="heading-copy">
        <span class="ai-icon"><Sparkles :size="19" /></span>
        <div>
          <h3>AI 风险摘要</h3>
          <p>基于合同字段差异与风险规则生成</p>
        </div>
      </div>
      <span class="risk-pill" :class="riskClass">{{ riskLabel }}</span>
    </header>

    <section class="overview-grid">
      <div class="score-card">
        <div class="donut" :style="donutStyle">
          <div>
            <strong>{{ matchRate }}%</strong><small>字段一致率</small>
          </div>
        </div>
        <div class="score-copy">
          <span>本次对比概览</span>
          <strong>{{ total }} 个关键字段完成核验</strong>
          <p>{{ mismatchCount }} 项差异，{{ highRiskCount }} 项高风险</p>
        </div>
      </div>
      <div class="risk-bars">
        <div v-for="item in riskBars" :key="item.key" class="bar-row">
          <span><i :class="item.key" />{{ item.label }}</span>
          <div>
            <i :class="item.key" :style="{ width: `${item.percent}%` }" />
          </div>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <div class="insight-grid">
      <section class="insight-card differences">
        <div class="subheading">
          <span><GitCompareArrows :size="17" /></span>
          <div>
            <h4>核心差异</h4>
            <p>优先呈现需要人工复核的合同字段</p>
          </div>
        </div>
        <ul v-if="importantDiffs.length" class="difference-list">
          <li
            v-for="(item, index) in importantDiffs"
            :key="`${item.field}-${index}`"
          >
            <span class="field-index">{{
              String(index + 1).padStart(2, "0")
            }}</span>
            <div>
              <strong>{{ fieldLabel(item.field) }}</strong
              ><small
                >{{ valueText(item.buy) }} → {{ valueText(item.sell) }}</small
              >
            </div>
            <em :class="riskTone(item.risk)">{{ riskText(item.risk) }}</em>
          </li>
        </ul>
        <p v-else class="empty-copy">关键字段一致，暂未发现需要复核的差异。</p>
      </section>

      <section class="insight-card warning">
        <div class="subheading">
          <span><ShieldAlert :size="17" /></span>
          <div>
            <h4>风险提示</h4>
            <p>按照风险等级聚合关键问题</p>
          </div>
        </div>
        <div class="warning-copy">
          <div
            v-for="(line, index) in riskNarratives"
            :key="index"
            class="risk-line"
          >
            <span
              v-if="riskBadge(line)"
              :class="`badge-${riskBadge(line)?.toLowerCase()}`"
            >
              {{ riskBadge(line) }}
            </span>
            <p>{{ cleanNarrative(line) }}</p>
          </div>
          <div v-if="!riskNarratives.length" class="risk-line">
            <p>{{ fallbackRiskText }}</p>
          </div>
        </div>
      </section>

      <section class="insight-card advice">
        <div class="subheading">
          <span><Lightbulb :size="17" /></span>
          <div>
            <h4>处理建议</h4>
            <p>将分析结果转化为可执行的复核动作</p>
          </div>
        </div>
        <ol>
          <li v-for="(line, index) in adviceNarratives" :key="index">
            <span>{{ index + 1 }}</span>
            <p>{{ line }}</p>
          </li>
        </ol>
      </section>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import {
  GitCompareArrows,
  Lightbulb,
  ShieldAlert,
  Sparkles,
} from "lucide-vue-next";

const props = defineProps<{
  summary?: string;
  differences?: any[];
  riskLevel?: string;
}>();
const differences = computed(() => props.differences || []);
const total = computed(() => differences.value.length);
const matchCount = computed(
  () => differences.value.filter((item) => item.status === "MATCH").length,
);
const mismatchCount = computed(
  () => differences.value.filter((item) => item.status !== "MATCH").length,
);
const highRiskCount = computed(
  () => differences.value.filter((item) => item.risk === "HIGH").length,
);
const matchRate = computed(() =>
  total.value ? Math.round((matchCount.value / total.value) * 100) : 100,
);
const donutStyle = computed(() => ({
  background: `conic-gradient(#1769e0 0 ${matchRate.value}%, #e7edf5 ${matchRate.value}% 100%)`,
}));
const riskLabel = computed(
  () =>
    ({ HIGH: "高风险", MEDIUM: "中风险", LOW: "低风险", NONE: "无明显风险" })[
      props.riskLevel || ""
    ] || "待评估",
);
const riskClass = computed(
  () => `risk-${(props.riskLevel || "none").toLowerCase()}`,
);
const riskBars = computed(() => {
  const denominator = Math.max(total.value, 1);
  return [
    {
      key: "high",
      label: "高风险",
      value: differences.value.filter((item) => item.risk === "HIGH").length,
    },
    {
      key: "medium",
      label: "中风险",
      value: differences.value.filter((item) => item.risk === "MEDIUM").length,
    },
    {
      key: "low",
      label: "低风险",
      value: differences.value.filter((item) => item.risk === "LOW").length,
    },
  ].map((item) => ({
    ...item,
    percent: Math.round((item.value / denominator) * 100),
  }));
});
const importantDiffs = computed(() =>
  differences.value.filter((item) => item.status !== "MATCH"),
);
const fieldMap: Record<string, string> = {
  applicationNo: "申请单编号",
  applicationTitle: "申请单标题",
  supplierName: "合同主体",
  itemName: "商品名称",
  itemModel: "规格型号",
  quantity: "采购数量",
  purchaseUnitPrice: "采购单价",
  purchaseTotalAmount: "采购总金额",
  expectedDeliveryDate: "预计交货日",
  paymentTerms: "付款方式",
  deliveryLocation: "交货地点",
};
function fieldLabel(field: string) {
  return fieldMap[field] || field || "未命名字段";
}
function valueText(value: unknown) {
  return value === null || value === undefined || value === ""
    ? "未提供"
    : String(value);
}
function riskText(risk: string) {
  return { HIGH: "高风险", MEDIUM: "中风险", LOW: "低风险" }[risk] || "需关注";
}
function riskTone(risk: string) {
  return `tone-${(risk || "medium").toLowerCase()}`;
}

type SummaryBucket = "intro" | "core" | "risk" | "advice";
function summaryBuckets(source = "") {
  const result: Record<SummaryBucket, string[]> = {
    intro: [],
    core: [],
    risk: [],
    advice: [],
  };
  let current: SummaryBucket = "intro";
  for (const raw of source.replace(/<[^>]+>/g, " ").split(/\r?\n/)) {
    const line = raw
      .replace(/^\s*#{1,6}\s*/, "")
      .replace(/^\s*(?:[-*+] |\d+[.)]\s*)/, "")
      .replace(/[*_`]/g, "")
      .trim();
    if (!line || /^[-|:\s]+$/.test(line)) continue;
    if (/^核心差异/.test(line)) {
      current = "core";
      continue;
    }
    if (/^风险提示/.test(line)) {
      current = "risk";
      continue;
    }
    if (/^(处理建议|建议)/.test(line)) {
      current = "advice";
      continue;
    }
    const tableCells = line.startsWith("|")
      ? line
          .split("|")
          .map((cell) => cell.trim())
          .filter(Boolean)
      : [];
    if (tableCells.length) {
      if (!/^(差异项|采购合同|销售合同)/.test(tableCells.join(" ")))
        result[current].push(tableCells.join(" · "));
      continue;
    }
    result[current].push(line);
  }
  return result;
}
const buckets = computed(() => summaryBuckets(props.summary));
const riskNarratives = computed(() => {
  if (buckets.value.risk.length) return buckets.value.risk;
  return buckets.value.intro.filter((line) =>
    /风险|不匹配|差异|缺失|异常|HIGH|MEDIUM|LOW/.test(line),
  );
});
const fallbackRiskText = computed(() =>
  highRiskCount.value
    ? `发现 ${highRiskCount.value} 项高风险差异，请在提交采购申请前完成复核。`
    : mismatchCount.value
      ? `发现 ${mismatchCount.value} 项字段差异，建议确认后再提交。`
      : "暂未发现明显合同风险。",
);
const adviceNarratives = computed(() => {
  const fromSummary = buckets.value.advice.length
    ? buckets.value.advice
    : buckets.value.intro.filter((line) =>
        /建议|应当|需要|请|复核|确认/.test(line),
      );
  if (fromSummary.length) return fromSummary;
  const fields = importantDiffs.value.map(
    (item) => `核对${fieldLabel(item.field)}的买卖合同约定，确认最终执行口径。`,
  );
  return fields.length
    ? fields
    : ["关键字段已通过一致性检查，可继续进行采购申请确认。"];
});
function riskBadge(line: string) {
  return line.match(/\b(HIGH|MEDIUM|LOW)\b/i)?.[1]?.toUpperCase() || "";
}
function cleanNarrative(line: string) {
  return line
    .replace(/^[🔴🟠🟡🟢⚠️\s]*/, "")
    .replace(/^(HIGH|MEDIUM|LOW)\b\s*[—–-]?\s*/i, "")
    .trim();
}
</script>

<style scoped>
.risk-summary {
  padding: 22px;
}
.summary-heading,
.heading-copy,
.subheading {
  display: flex;
  align-items: center;
}
.summary-heading {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.heading-copy {
  gap: 11px;
}
.ai-icon,
.subheading > span {
  display: grid;
  place-items: center;
  background: #e8f1ff;
  color: var(--primary);
}
.ai-icon {
  width: 38px;
  height: 38px;
  border-radius: 13px;
}
.summary-heading h3,
.subheading h4 {
  margin: 0;
}
.summary-heading h3 {
  font-size: 15px;
}
.summary-heading p,
.subheading p {
  margin: 2px 0 0;
  color: var(--muted-foreground);
  font-size: 11px;
}
.risk-pill {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}
.risk-high {
  background: #ffeaeb;
  color: #cf3444;
}
.risk-medium {
  background: #fff3dc;
  color: #b56800;
}
.risk-low,
.risk-none {
  background: #e7f8ef;
  color: #087a55;
}
.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 0.95fr);
  gap: 14px;
  margin-bottom: 14px;
}
.score-card,
.risk-bars {
  border: 1px solid #e8edf4;
  border-radius: 18px;
  background: #f8fafc;
}
.score-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px;
}
.donut {
  width: 112px;
  height: 112px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  transition: background 0.5s ease;
}
.donut > div {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 50%;
  background: white;
  box-shadow: inset 0 0 0 1px #edf1f6;
}
.donut strong {
  font-size: 22px;
  letter-spacing: -0.04em;
}
.donut small,
.score-copy span {
  color: var(--muted-foreground);
  font-size: 10px;
}
.score-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.score-copy strong {
  font-size: 14px;
}
.score-copy p {
  margin: 0;
  color: var(--muted-foreground);
  font-size: 12px;
}
.risk-bars {
  display: flex;
  justify-content: center;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
}
.bar-row {
  display: grid;
  grid-template-columns: 72px 1fr 20px;
  align-items: center;
  gap: 10px;
  font-size: 11px;
}
.bar-row > span {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bar-row > span i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.bar-row > div {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: #e7edf5;
}
.bar-row > div i {
  display: block;
  min-width: 3px;
  height: 100%;
  border-radius: inherit;
  transition: width 0.6s ease;
}
.high {
  background: #ef5262;
}
.medium {
  background: #f5a524;
}
.low {
  background: #28b887;
}
.bar-row strong {
  text-align: right;
}
.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}
.insight-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 17px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: #fff;
}
.differences {
  border-color: #dce9fb;
  background: linear-gradient(155deg, #f3f8ff, #fbfdff);
}
.subheading {
  gap: 10px;
  margin-bottom: 16px;
}
.subheading > span {
  width: 34px;
  height: 34px;
  border-radius: 11px;
}
.subheading h4 { font-size: 15px; font-weight: 700; color: #1e293b; }
.subheading p { font-size: 12px; line-height: 1.5; color: #64748b; }
.difference-list,
.advice ol {
  list-style: none;
  margin: 0;
  padding: 0;
}
.difference-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.difference-list li {
  display: grid;
  grid-template-columns: 30px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.72);
}
.field-index {
  color: #93a0b1;
  font: 600 10px var(--font-mono);
}
.difference-list li > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}
.difference-list strong {
  font-size: 13px;
  line-height: 1.45;
  color: #243247;
}
.difference-list small {
  margin-top: 3px;
  white-space: normal;
  overflow-wrap: anywhere;
  color: #526176;
  font-size: 12px;
  line-height: 1.5;
}
.difference-list em {
  padding: 4px 7px;
  border-radius: 999px;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}
.tone-high {
  background: #ffeaeb;
  color: #cf3444;
}
.tone-medium {
  background: #fff3dc;
  color: #b56800;
}
.tone-low {
  background: #e7f8ef;
  color: #087a55;
}
.warning {
  border-color: #f4dfe2;
  background: linear-gradient(155deg, #fff4f5, #fffafa);
}
.warning .subheading > span {
  background: #ffe5e8;
  color: #c93648;
}
.warning-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.risk-line {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(201, 54, 72, 0.12);
}
.risk-line:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}
.risk-line > span {
  flex: 0 0 auto;
  padding: 3px 5px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 900;
}
.badge-high {
  background: #ffeaeb;
  color: #c82f40;
}
.badge-medium {
  background: #fff0d7;
  color: #aa6400;
}
.badge-low {
  background: #e5f8ef;
  color: #087a55;
}
.risk-line p,
.empty-copy {
  margin: 0;
  color: #344258;
  font-size: 13px;
  line-height: 1.65;
}
.advice .subheading > span {
  background: #fff0cf;
  color: #b66a00;
}
.advice {
  border-color: #f0e3c5;
  background: linear-gradient(155deg, #fff9eb, #fffdf7);
}
.advice ol {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.advice li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.advice li > span {
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: #fff0cf;
  color: #a65f00;
  font-size: 11px;
  font-weight: 800;
}
.advice li p {
  margin: 0;
  color: #344258;
  font-size: 13px;
  line-height: 1.65;
}
@media (max-width: 900px) {
  .overview-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 560px) {
  .risk-summary {
    padding: 16px;
  }
  .score-card {
    align-items: flex-start;
  }
  .donut {
    width: 90px;
    height: 90px;
  }
  .donut > div {
    width: 64px;
    height: 64px;
  }
  .donut strong {
    font-size: 18px;
  }
}
</style>
