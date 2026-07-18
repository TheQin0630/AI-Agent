<template>
  <AppShell
    :navs="navs"
    search-placeholder="搜索申请单编号、供应商、商品…"
    v-model:search="searchQuery"
  >
    <header class="hero page-hero">
      <div>
        <p class="section-kicker">Procurement Center</p>
        <h2>采购申请</h2>
        <p>统一管理采购申请、跟踪处理进度并完成人工审核。</p>
      </div>
      <button class="action-btn primary" @click="$router.push('/tasks/new')">
        <Plus :size="16" />新建采购任务
      </button>
    </header>

    <section class="metrics-grid" aria-label="申请单概览">
      <MetricCard
        label="全部申请单"
        :value="allTotal"
        suffix="单"
        hint="当前累计申请数量"
        :icon="FileText"
        :active="activeFilter === '全部'"
        @click="activeFilter = '全部'"
      />
      <MetricCard
        label="待处理"
        :value="pendingCount"
        suffix="单"
        hint="等待人工确认"
        :icon="Clock3"
        tone="amber"
        :active="activeFilter === '待确认'"
        @click="activeFilter = '待确认'"
      />
      <MetricCard
        label="已确认申请"
        :value="submittedCount"
        suffix="单"
        hint="已完成人工确认"
        :icon="CircleCheckBig"
        tone="green"
        :active="activeFilter === '已确认'"
        @click="activeFilter = '已确认'"
      />
    </section>

    <WorkflowProgress
      :steps="steps"
      :active="workflowActive"
      :caption="`${submittedCount}/${Math.max(total, submittedCount)} 已确认`"
    />

    <article class="content-card list-card">
      <div class="list-toolbar">
        <div>
          <h3>申请单明细</h3>
          <p>点击任意申请单查看详情与审核信息</p>
        </div>
        <FilterChips :options="filters" v-model="activeFilter" />
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>申请单编号</th>
              <th>标题 / 供应商</th>
              <th>状态</th>
              <th>商品</th>
              <th>流程进度</th>
              <th>创建时间</th>
              <th aria-label="操作" />
            </tr>
          </thead>
          <tbody v-if="loading" aria-busy="true">
            <tr v-for="n in 5" :key="n">
              <td v-for="m in 7" :key="m">
                <div
                  class="skeleton"
                  :style="{ height: '16px', width: m === 2 ? '150px' : '76px' }"
                />
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(e, index) in visibleItems"
              :key="e.id"
              class="data-row"
              :style="{ '--row-index': index }"
              tabindex="0"
              @click="open(e.id)"
              @keydown.enter="open(e.id)"
            >
              <td class="cell-mono whitespace-nowrap">
                {{ e.applicationNo || "—" }}
              </td>
              <td>
                <div class="primary-cell">
                  <strong>{{ applicationDisplayTitle(e) }}</strong
                  ><span>{{ safe(e.supplierName, "供应商待补充") }}</span>
                </div>
              </td>
              <td><ExtractionStatusBadge :status="e.applicationStatus" /></td>
              <td>{{ safe(e.itemName, "商品待补充") }}</td>
              <td>
                <RowProgress
                  :value="
                    e.workflow?.progress ?? progressOf(e.applicationStatus)
                  "
                />
              </td>
              <td class="cell-mono whitespace-nowrap">
                {{ fmt(e.createTime) }}
              </td>
              <td>
                <button
                  class="row-action"
                  type="button"
                  aria-label="查看详情"
                  @click.stop="open(e.id)"
                >
                  <ChevronRight :size="17" />
                </button>
              </td>
            </tr>
            <tr v-if="!visibleItems.length">
              <td colspan="7">
                <div class="empty-state">
                  <Inbox :size="30" /><strong>暂无申请单</strong
                  ><span>新的采购申请将在这里展示</span>
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
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import dayjs from "dayjs";
import {
  ChevronRight,
  CircleCheckBig,
  ClipboardList,
  Clock3,
  FileText,
  Inbox,
  ListTodo,
  Plus,
} from "lucide-vue-next";
import AppShell from "@/components/AppShell.vue";
import MetricCard from "@/components/MetricCard.vue";
import WorkflowProgress from "@/components/WorkflowProgress.vue";
import RowProgress from "@/components/RowProgress.vue";
import FilterChips from "@/components/FilterChips.vue";
import Pagination from "@/components/Pagination.vue";
import ExtractionStatusBadge from "@/components/ExtractionStatusBadge.vue";
import { useRouteFilter } from "@/composables/useRouteFilter";
import { extractionApi, type ExtractionListItem } from "@/api/extraction";
const router = useRouter();
const route = useRoute();
const navs = [
  { key: "tasks", label: "合同对比", icon: ListTodo, to: "/tasks" },
  {
    key: "extractions",
    label: "采购申请单",
    icon: ClipboardList,
    to: "/extractions",
  },
];
const filters = ["全部", "待确认", "已确认", "审批驳回"];
const activeFilter = useRouteFilter(
  filters,
  () => route.query.status,
  (status) => {
    void router.replace({ query: { ...route.query, status } });
  },
);
const items = ref<ExtractionListItem[]>([]);
const overviewItems = ref<ExtractionListItem[]>([]);
const allTotal = ref(0);
const total = ref(0);
const page = ref(1);
const loading = ref(true);
const searchQuery = ref(String(route.query.q || ""));
const visibleItems = computed(() => {
  const keyword = searchQuery.value.trim().toLocaleLowerCase();
  if (!keyword) return items.value;
  return items.value.filter((item) =>
    [
      item.applicationNo,
      item.applicationTitle,
      item.supplierName,
      item.itemName,
      item.applicationStatus,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLocaleLowerCase().includes(keyword)),
  );
});
const submittedCount = computed(
  () =>
    overviewItems.value.filter((e) => e.applicationStatus === "已确认")
      .length,
);
const pendingCount = computed(
  () =>
    overviewItems.value.filter((e) => e.applicationStatus === "待确认").length,
);
const workflowActive = computed(() =>
  submittedCount.value ? 3 : overviewItems.value.length ? 2 : 0,
);
const steps = computed(() => [
  { label: "资料解析", detail: `${allTotal.value} 单` },
  { label: "合同比对", detail: "自动检查" },
  { label: "人工确认", detail: `${pendingCount.value} 待处理` },
  { label: "订单处理", detail: `${submittedCount.value} 已完成` },
]);
function fmt(s: string | null) {
  return s ? dayjs(s).format("YYYY-MM-DD HH:mm") : "—";
}
function safe(value: string | null, fallback: string) {
  return !value || /^\?+$/.test(value) ? fallback : value;
}
function applicationDisplayTitle(item: ExtractionListItem) {
  const supplier = safe(item.supplierName, "");
  const product = safe(item.itemName, "");
  const fallback = [supplier, product].filter(Boolean).join("-") || "采购申请单";
  return safe(item.applicationTitle, fallback);
}
function progressOf(status: string) {
  if (status === "已确认") return 100;
  if (status === "待确认") return 75;
  if (status === "审批驳回") return 45;
  return 50;
}
function open(id: number) {
  router.push(`/extractions/${id}/confirm`);
}
async function load() {
  loading.value = true;
  try {
    const res: any = await extractionApi.list({
      page: page.value,
      size: 20,
      status: activeFilter.value === "全部" ? undefined : activeFilter.value,
    });
    items.value = res.data.items;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}
async function loadOverview() {
  const res: any = await extractionApi.list({ page: 1, size: 100 });
  overviewItems.value = res.data.items || [];
  allTotal.value = res.data.total || 0;
}
watch(() => route.query.status, () => {
  page.value = 1;
  load();
});
onMounted(() => {
  loadOverview();
  load();
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
.data-table {
  min-width: 980px;
}
.data-table td:nth-child(4) {
  min-width: 110px;
  white-space: nowrap;
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
.primary-cell {
  display: flex;
  flex-direction: column;
  max-width: 260px;
}
.primary-cell strong {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.primary-cell span {
  margin-top: 3px;
  color: var(--muted-foreground);
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-action {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: transparent;
  color: var(--muted-foreground);
  cursor: pointer;
  transition: all 0.2s;
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
  gap: 7px;
  padding: 44px;
  color: var(--muted-foreground);
}
.empty-state strong {
  color: var(--foreground);
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
  .page-hero .action-btn {
    width: 100%;
  }
}
</style>
