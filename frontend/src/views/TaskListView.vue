<template>
  <AppShell
    :navs="navs"
    search-placeholder="搜索任务名称、合同编号…"
    v-model:search="searchQuery"
  >
    <header class="hero page-hero">
      <div>
        <p class="section-kicker">Contract Intelligence</p>
        <h2>合同对比任务</h2>
        <p>追踪合同解析、智能比对与风险复核的全过程。</p>
      </div>
      <div class="hero-actions">
        <button class="action-btn primary" @click="$router.push('/tasks/new')">
          <Plus :size="16" />新建对比任务</button
        ><button
          class="action-btn ghost"
          :disabled="exporting"
          @click="exportTaskList"
        >
          <Download :size="16" />{{ exporting ? "正在导出…" : "导出任务清单" }}
        </button>
      </div>
    </header>
    <section class="metrics-grid">
      <MetricCard
        label="全部任务"
        :value="allTotal"
        suffix="项"
        hint="累计合同对比任务"
        :icon="Files"
        :active="activeFilter === '全部'"
        @click="activeFilter = '全部'"
      /><MetricCard
        label="正在处理"
        :value="running"
        suffix="项"
        hint="AI 正在解析或比对"
        :icon="LoaderCircle"
        tone="amber"
        :active="activeFilter === '进行中'"
        @click="activeFilter = '进行中'"
      /><MetricCard
        label="已完成"
        :value="done"
        suffix="项"
        hint="可查看风险报告"
        :icon="BadgeCheck"
        tone="green"
        :active="activeFilter === '已完成'"
        @click="activeFilter = '已完成'"
      />
    </section>
    <article class="content-card list-card">
      <div class="list-toolbar">
        <div>
          <h3>任务明细</h3>
          <p>实时呈现任务阶段和风险状态</p>
        </div>
        <FilterChips :options="filters" v-model="activeFilter" />
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>状态</th>
              <th>风险等级</th>
              <th>处理进度</th>
              <th>创建时间</th>
              <th>更新时间</th>
              <th />
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr v-for="n in 5" :key="n">
              <td v-for="m in 7" :key="m">
                <div class="skeleton" style="width: 82px; height: 16px" />
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(t, index) in visibleTasks"
              :key="t.id"
              class="data-row"
              :style="{ '--row-index': index }"
              tabindex="0"
              @click="open(t.id)"
              @keydown.enter="open(t.id)"
            >
              <td>
                <div class="task-name">
                  <span><FileSearch :size="17" /></span
                  ><strong>{{ t.title }}</strong>
                </div>
              </td>
              <td><StatusBadge :status="t.status" /></td>
              <td><RiskBadge :level="t.riskLevel" /></td>
              <td>
                <RowProgress
                  :value="t.workflow?.progress ?? progressOf(t.status)"
                />
              </td>
              <td class="cell-mono">{{ fmt(t.createdAt) }}</td>
              <td class="cell-mono">{{ fmt(t.updatedAt) }}</td>
              <td class="actions-cell">
                <button
                  v-if="t.status === 'FAILED'"
                  class="retry-action"
                  type="button"
                  :disabled="retryingIds.has(t.id)"
                  :aria-label="`重新分析任务：${t.title}`"
                  @click.stop="retryTask(t)"
                >
                  <LoaderCircle
                    v-if="retryingIds.has(t.id)"
                    class="retry-spinner"
                    :size="15"
                  />
                  <RotateCcw v-else :size="15" />
                  {{ retryingIds.has(t.id) ? "提交中" : "重新分析" }}
                </button>
                <button
                  class="row-action"
                  type="button"
                  aria-label="查看任务"
                  @click.stop="open(t.id)"
                >
                  <ChevronRight :size="17" />
                </button>
              </td>
            </tr>
            <tr v-if="!visibleTasks.length">
              <td colspan="7">
                <div class="empty-state">
                  <Files :size="30" /><strong>暂无合同对比任务</strong
                  ><span>上传采购合同与销售合同后开始智能分析</span
                  ><button
                    class="action-btn primary"
                    @click="$router.push('/tasks/new')"
                  >
                    创建第一个任务
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination
        :total="total"
        :page="page"
        :size="20"
        @update:page="
          page = $event;
          load();
        "
      />
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import dayjs from "dayjs";
import {
  BadgeCheck,
  ChevronRight,
  ClipboardList,
  Download,
  FileSearch,
  Files,
  ListTodo,
  LoaderCircle,
  Plus,
  RotateCcw,
} from "lucide-vue-next";
import AppShell from "@/components/AppShell.vue";
import MetricCard from "@/components/MetricCard.vue";
import FilterChips from "@/components/FilterChips.vue";
import Pagination from "@/components/Pagination.vue";
import RowProgress from "@/components/RowProgress.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import RiskBadge from "@/components/RiskBadge.vue";
import { useRouteFilter } from "@/composables/useRouteFilter";
import { taskApi } from "@/api/task";
import { useToastStore } from "@/stores/toast";
const router = useRouter(),
  route = useRoute(),
  toast = useToastStore();
const navs = [
  { key: "tasks", label: "合同对比", icon: ListTodo, to: "/tasks" },
  {
    key: "extractions",
    label: "采购申请单",
    icon: ClipboardList,
    to: "/extractions",
  },
];
const filters = ["全部", "已完成", "进行中", "待处理", "失败"];
const activeFilter = useRouteFilter(
  filters,
  () => route.query.status,
  (status) => {
    void router.replace({ query: { ...route.query, status } });
  },
);
const tasks = ref<any[]>([]),
  overviewTasks = ref<any[]>([]),
  allTotal = ref(0),
  total = ref(0),
  page = ref(1),
  loading = ref(true),
  exporting = ref(false),
  retryingIds = ref(new Set<number>());
let pollTimer: ReturnType<typeof window.setInterval> | undefined;
let refreshing = false;
const searchQuery = ref(String(route.query.q || ""));
const statusMap: Record<string, string | undefined> = {
  全部: undefined,
  已完成: undefined,
  进行中: "RUNNING",
  待处理: "PENDING",
  失败: "FAILED",
};
function applyTaskFilter(list: any[], filter: string) {
  if (filter === "已完成")
    return list.filter((task) => ["DONE", "CONFIRMED"].includes(task.status));
  return list;
}
const running = computed(
    () => overviewTasks.value.filter((t) => t.status === "RUNNING").length,
  ),
  done = computed(
    () =>
      overviewTasks.value.filter((t) =>
        ["DONE", "CONFIRMED"].includes(t.status),
      ).length,
  );
const visibleTasks = computed(() => {
  const keyword = searchQuery.value.trim().toLocaleLowerCase();
  if (!keyword) return tasks.value;
  return tasks.value.filter((task) =>
    [task.title, task.id, task.status, task.riskLevel]
      .filter((value) => value !== null && value !== undefined)
      .some((value) => String(value).toLocaleLowerCase().includes(keyword)),
  );
});
function progressOf(s: string) {
  return (
    { PENDING: 15, RUNNING: 58, DONE: 90, CONFIRMED: 100, FAILED: 35 }[s] ?? 10
  );
}
function fmt(s: string) {
  return s ? dayjs(s).format("YYYY-MM-DD HH:mm") : "—";
}
function open(id: number) {
  router.push(`/tasks/${id}`);
}
async function retryTask(task: any) {
  if (task.status !== "FAILED" || retryingIds.value.has(task.id)) return;
  retryingIds.value = new Set(retryingIds.value).add(task.id);
  try {
    await taskApi.retry(task.id);
    task.status = "RUNNING";
    toast.success(`“${task.title}”已开始重新分析`);
    await Promise.all([load(true), loadOverview()]);
  } finally {
    const next = new Set(retryingIds.value);
    next.delete(task.id);
    retryingIds.value = next;
  }
}
async function load(silent = false) {
  if (refreshing) return;
  refreshing = true;
  if (!silent) loading.value = true;
  try {
    const groupedFilter = activeFilter.value === "已完成";
    const res: any = await taskApi.list({
      page: page.value,
      size: groupedFilter ? 100 : 20,
      status: statusMap[activeFilter.value],
    });
    tasks.value = applyTaskFilter(res.data.items || [], activeFilter.value);
    total.value = groupedFilter ? tasks.value.length : res.data.total;
  } finally {
    if (!silent) loading.value = false;
    refreshing = false;
  }
}
async function loadOverview() {
  const res: any = await taskApi.list({ page: 1, size: 100 });
  overviewTasks.value = res.data.items || [];
  allTotal.value = res.data.total || 0;
}
function csvCell(value: unknown) {
  return `"${String(value ?? "").replace(/"/g, '""')}"`;
}
async function exportTaskList() {
  exporting.value = true;
  try {
    const res: any = await taskApi.list({
      page: 1,
      size: 100,
      status: statusMap[activeFilter.value],
    });
    const rows = applyTaskFilter(res.data.items || [], activeFilter.value).map(
      (task: any) =>
        [
          task.id,
          task.title,
          task.status,
          task.riskLevel,
          task.workflow?.progress ?? progressOf(task.status),
          fmt(task.createdAt),
          fmt(task.updatedAt),
        ]
          .map(csvCell)
          .join(","),
    );
    const csv = [
      "任务ID,任务名称,状态,风险等级,处理进度,创建时间,更新时间",
      ...rows,
    ].join("\r\n");
    const url = URL.createObjectURL(
      new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" }),
    );
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `合同对比任务-${dayjs().format("YYYYMMDD-HHmm")}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
    toast.success(`已导出 ${rows.length} 条任务`);
  } finally {
    exporting.value = false;
  }
}
watch(() => route.query.status, () => {
  page.value = 1;
  load();
});
onMounted(() => {
  loadOverview();
  load();
  pollTimer = window.setInterval(() => {
    if (document.visibilityState === "visible") {
      void Promise.all([load(true), loadOverview()]);
    }
  }, 10_000);
});
onBeforeUnmount(() => {
  if (pollTimer !== undefined) window.clearInterval(pollTimer);
});
</script>
<style scoped>
.page-hero {
  align-items: flex-end;
}
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.list-card {
  border-radius: 22px;
}
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
}
.list-toolbar h3 {
  margin: 0;
  font-size: 15px;
}
.list-toolbar p {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--muted-foreground);
}
.data-row {
  cursor: pointer;
  animation: row-in 0.4s cubic-bezier(0.2, 0.8, 0.2, 1) both;
  animation-delay: calc(var(--row-index) * 35ms);
}
@keyframes row-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
}
.task-name {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
}
.task-name > span {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: #e8f1ff;
  color: var(--primary);
}
.task-name strong {
  font-size: 13px;
}
.row-action {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: var(--muted-foreground);
  display: grid;
  place-items: center;
  transition: all 0.2s;
}
.actions-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  white-space: nowrap;
}
.retry-action {
  min-width: 94px;
  height: 32px;
  padding: 0 11px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fff7f7;
  color: #dc2626;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}
.retry-action:hover:not(:disabled) {
  border-color: #fca5a5;
  background: #feecec;
}
.retry-action:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
.retry-action:disabled {
  cursor: wait;
  opacity: 0.65;
}
.retry-spinner {
  animation: retry-spin 1s linear infinite;
}
@keyframes retry-spin {
  to {
    transform: rotate(360deg);
  }
}
.data-row:hover .row-action {
  background: #e8f1ff;
  color: var(--primary);
  transform: translateX(2px);
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 42px;
  color: var(--muted-foreground);
}
.empty-state strong {
  color: var(--foreground);
}
.empty-state .action-btn {
  margin-top: 8px;
}
@media (max-width: 980px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .list-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
@media (max-width: 760px) {
  .page-hero {
    align-items: flex-start;
  }
}
</style>
