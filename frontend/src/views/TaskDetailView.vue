<template>
  <AppShell :navs="navs" search-placeholder="搜索任务…">
    <main v-if="d" class="detail-page">
      <nav class="breadcrumb">
        <router-link to="/tasks"><ChevronLeft :size="15" />合同对比</router-link
        ><span>/</span><strong>{{ d.title }}</strong>
      </nav>
      <header class="detail-header">
        <div>
          <p class="section-kicker">Task #{{ d.id }}</p>
          <div class="title-line">
            <h2>{{ d.title }}</h2>
            <StatusBadge :status="d.status" /><RiskBadge :level="d.riskLevel" />
          </div>
          <p>{{ statusHint }}</p>
        </div>
        <div class="hero-actions">
          <button class="action-btn ghost" @click="onExport">
            <Download :size="16" />导出报告</button
          ><button
            v-if="d.status === 'FAILED'"
            class="action-btn ghost"
            @click="onRetry"
          >
            <RotateCcw :size="16" />重新分析</button
          ><button
            v-if="d.status === 'DONE'"
            class="action-btn primary"
            @click="onConfirm"
          >
            <Check :size="16" />审批通过并创建采购申请
          </button>
        </div>
      </header>
      <WorkflowProgress
        :steps="steps"
        :active="activeStep"
        :caption="statusHint"
      />
      <section
        v-if="d.status === 'RUNNING' || d.status === 'PENDING'"
        class="processing-banner"
      >
        <span class="processing-icon"><LoaderCircle :size="20" /></span>
        <div>
          <strong>AI 正在处理合同</strong>
          <p>页面会自动呈现最新结果，你可以稍后回来查看。</p>
        </div>
        <div class="processing-track"><i /></div>
      </section>
      <RiskSummaryPanel
        :summary="d.summary"
        :differences="d.differences"
        :risk-level="d.riskLevel"
      />
      <section class="contracts-grid">
        <ContractCard
          side="BUY"
          :ex="d.buy?.extraction"
          :filename="d.buy?.filename"
        /><ContractCard
          side="SELL"
          :ex="d.sell?.extraction"
          :filename="d.sell?.filename"
        />
      </section>
      <article class="detail-surface">
        <div class="card-heading with-divider">
          <span><Search :size="18" /></span>
          <div>
            <h3>字段差异对比</h3>
            <p>重点字段与风险项已自动标记</p>
          </div>
        </div>
        <FieldDiffTable :diffs="d.differences" />
      </article>
      <article class="detail-surface chat-surface">
        <div class="card-heading with-divider">
          <span><MessageCircle :size="18" /></span>
          <div>
            <h3>对话追问</h3>
            <p>围绕本次合同分析继续询问</p>
          </div>
        </div>
        <ChatPanel :task-id="Number(route.params.id)" />
      </article>
    </main>
    <div v-else class="detail-skeleton">
      <div class="skeleton hero-skeleton" />
      <div class="skeleton card-skeleton" />
      <div class="skeleton card-skeleton" />
    </div>
  </AppShell>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import {
  Check,
  ChevronLeft,
  ClipboardList,
  Download,
  ListTodo,
  LoaderCircle,
  MessageCircle,
  RotateCcw,
  Search,
} from "lucide-vue-next";
import AppShell from "@/components/AppShell.vue";
import WorkflowProgress from "@/components/WorkflowProgress.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import RiskBadge from "@/components/RiskBadge.vue";
import FieldDiffTable from "@/components/FieldDiffTable.vue";
import ChatPanel from "@/components/ChatPanel.vue";
import ContractCard from "@/components/ContractCard.vue";
import RiskSummaryPanel from "@/components/RiskSummaryPanel.vue";
import { taskApi } from "@/api/task";
import { useToastStore } from "@/stores/toast";
const route = useRoute(),
  toast = useToastStore(),
  d = ref<any>(null);
const navs = [
  { key: "tasks", label: "合同对比", icon: ListTodo, to: "/tasks" },
  {
    key: "extractions",
    label: "采购申请单",
    icon: ClipboardList,
    to: "/extractions",
  },
];
const statusHint = computed(
  () =>
    d.value?.workflow?.nextAction ||
    {
      PENDING: "任务已创建，等待开始处理",
      RUNNING: "正在解析合同并执行字段比对",
      DONE: "分析完成，等待人工确认",
      CONFIRMED: "合同对比已审批通过，采购申请等待人工确认",
      FAILED: "分析失败，可重新发起任务",
    }[d.value?.status as string] ||
    "处理中",
);
const activeStep = computed(
  () =>
    ({ PENDING: 0, RUNNING: 1, DONE: 2, CONFIRMED: 3, FAILED: 1 })[
      d.value?.status as string
    ] ?? 0,
);
const steps = [
  { label: "上传合同", detail: "文件已接收" },
  { label: "智能比对", detail: "字段与规则" },
  { label: "风险复核", detail: "人工确认" },
  { label: "采购申请", detail: "等待人工确认" },
];
async function load() {
  const res: any = await taskApi.detail(Number(route.params.id));
  d.value = res.data;
}
async function onExport() {
  const res: any = await taskApi.report(Number(route.params.id));
  const blob = new Blob([res.data || res], { type: "text/markdown" }),
    url = URL.createObjectURL(blob),
    a = document.createElement("a");
  a.href = url;
  a.download = `${d.value.title}.md`;
  a.click();
  URL.revokeObjectURL(url);
}
async function onRetry() {
  await taskApi.retry(Number(route.params.id));
  toast.success("已重新发起分析");
  await load();
}
async function onConfirm() {
  const res: any = await taskApi.confirm(Number(route.params.id));
  const result = res.data;
  toast.success(
    result.applicationNo
      ? `采购申请 ${result.applicationNo} 已创建，请前往采购申请确认`
      : result.message || "审批处理完成",
  );
  await load();
}
onMounted(load);
</script>
<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--muted-foreground);
  font-size: 12px;
}
.breadcrumb a {
  display: flex;
  align-items: center;
  color: inherit;
  text-decoration: none;
}
.breadcrumb strong {
  max-width: 50vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--foreground);
}
.detail-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.title-line h2 {
  margin: 0;
  font-size: 25px;
  letter-spacing: -0.025em;
}
.detail-header p:last-child {
  margin: 7px 0 0;
  color: var(--muted-foreground);
}
.processing-banner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid #bcd8ff;
  background: #f2f7ff;
  overflow: hidden;
}
.processing-icon {
  width: 38px;
  height: 38px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  background: white;
  color: var(--primary);
}
.processing-icon svg {
  animation: spin 1.5s linear infinite;
}
.processing-banner strong {
  font-size: 13px;
}
.processing-banner p {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--muted-foreground);
}
.processing-track {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: #dbeafe;
}
.processing-track i {
  display: block;
  width: 40%;
  height: 100%;
  background: var(--primary);
  animation: processing 1.8s ease-in-out infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@keyframes processing {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(350%);
  }
}
.summary-card {
  padding: 22px;
}
.card-heading {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 16px;
}
.card-heading.with-divider {
  padding: 17px 20px;
  margin: 0;
  border-bottom: 1px solid #edf1f6;
}
.detail-surface {
  overflow: hidden;
  border: 0 !important;
  outline: 0;
  border-radius: 20px;
  background: #fff;
  box-shadow: none !important;
}
.chat-surface {
  overflow: hidden;
}
.card-heading > span {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: #e8f1ff;
  color: var(--primary);
}
.card-heading h3 {
  margin: 0;
  font-size: 14px;
}
.card-heading p {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--muted-foreground);
}
.summary-body {
  font-size: 14px;
  line-height: 1.8;
}
.summary-body :deep(p) {
  margin: 0.45em 0;
}
.contracts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.detail-skeleton {
  display: grid;
  gap: 18px;
}
.hero-skeleton {
  height: 100px;
}
.card-skeleton {
  height: 230px;
}
@media (max-width: 900px) {
  .contracts-grid {
    grid-template-columns: 1fr;
  }
  .detail-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
