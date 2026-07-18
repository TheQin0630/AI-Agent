<template>
  <AppShell :navs="navs" search-placeholder="搜索采购申请单…">
    <main v-if="d" class="confirm-page">
      <header class="detail-header">
        <div>
          <div class="title-line">
            <h2>{{ title }}</h2>
            <ExtractionStatusBadge :status="d.applicationStatus" />
          </div>
        </div>
        <div class="hero-actions">
          <router-link class="action-btn ghost" to="/extractions">
            <ChevronLeft :size="18" />返回列表
          </router-link>
          <template v-if="d.applicationStatus === '待确认'">
            <button class="action-btn ghost danger" @click="showReject = true">
              <X :size="16" />驳回补充</button
            ><button
              class="action-btn primary"
              :disabled="confirming"
              @click="onConfirm"
            >
              <LoaderCircle v-if="confirming" class="spin" :size="16" /><Check
                v-else
                :size="16"
              />{{ confirming ? "正在确认…" : "确认申请单" }}
            </button>
          </template>
        </div>
      </header>
      <WorkflowProgress
        :steps="steps"
        :active="activeStep"
        :caption="d.applicationStatus"
      />
      <section class="info-grid">
        <article class="info-card wide">
          <div class="card-heading">
            <FileText :size="21" />
            <h3>申请单信息</h3>
          </div>
          <dl class="detail-grid">
            <Info
              label="申请单编号"
              :value="d.applicationNo || '—'"
              mono
            /><Info label="申请单标题" :value="title" /><Info
              label="申请类型"
              :value="d.applicationType || '—'"
            /><Info
              label="申请日期"
              :value="d.applyDate || '—'"
            /><Info label="申请人信息" :value="clean(d.applicantName)" /><Info
              label="创建时间"
              :value="createdAt"
            />
          </dl>
        </article>
        <article class="info-card">
          <div class="card-heading">
            <Building2 :size="21" />
            <h3>供应商信息</h3>
          </div>
          <dl class="detail-grid supplier-grid">
            <Info label="供应商名称" :value="clean(d.supplierName)" /><Info
              label="发货方式"
              :value="clean(d.shippingMethod)"
            /><Info
              label="预计交货时间"
              :value="d.expectedDeliveryDate || '—'"
            /><Info label="付款条款" :value="clean(d.paymentTerms)"
            />
          </dl>
        </article>
        <article class="info-card">
          <div class="card-heading">
            <Package :size="21" />
            <h3>商品信息</h3>
          </div>
          <dl class="detail-grid product-grid">
            <Info label="商品名称" :value="clean(d.itemName)" /><Info
              label="规格型号"
              :value="clean(d.itemModel)"
            /><Info label="单位" :value="d.unit || '—'" /><Info
              label="采购单价"
              :value="unitPrice"
            /><Info label="数量" :value="d.quantity ?? '—'" /><Info
              label="币种名称"
              :value="clean(d.currency)"
            /><Info label="含税采购金额"
              :value="money"
              highlight
            /><Info label="税率名称" :value="clean(d.taxRateName)" />
          </dl>
        </article>
      </section>
      <article v-if="d.message" class="info-card note-card">
        <MessageSquare :size="18" />
        <div>
          <strong>处理备注</strong>
          <p>{{ d.message }}</p>
        </div>
      </article>
    </main>
    <div v-else class="detail-skeleton">
      <div class="skeleton" style="height: 110px" />
      <div class="skeleton" style="height: 280px" />
    </div>
    <transition name="modal"
      ><div
        v-if="showReject"
        class="modal-backdrop"
        @click.self="showReject = false"
      >
        <section
          class="modal-card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="reject-title"
        >
          <div class="modal-icon"><AlertTriangle :size="21" /></div>
          <h3 id="reject-title">驳回并请求补充</h3>
          <p>请说明需要修正或补充的内容，系统将生成补充请求。</p>
          <textarea
            v-model="rejectReason"
            rows="4"
            placeholder="例如：供应商名称与合同主体不一致，请重新确认…"
            autofocus
          />
          <div class="modal-actions">
            <button class="action-btn ghost" @click="showReject = false">
              取消</button
            ><button
              class="action-btn primary danger-fill"
              :disabled="!rejectReason.trim() || rejecting"
              @click="onReject"
            >
              {{ rejecting ? "正在提交…" : "确认驳回" }}
            </button>
          </div>
        </section>
      </div></transition
    >
  </AppShell>
</template>
<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  AlertTriangle,
  Building2,
  Check,
  ChevronLeft,
  ClipboardList,
  FileText,
  ListTodo,
  LoaderCircle,
  MessageSquare,
  Package,
  X,
} from "lucide-vue-next";
import AppShell from "@/components/AppShell.vue";
import WorkflowProgress from "@/components/WorkflowProgress.vue";
import ExtractionStatusBadge from "@/components/ExtractionStatusBadge.vue";
import { extractionApi, type ExtractionDetail } from "@/api/extraction";
import { useToastStore } from "@/stores/toast";
const Info = defineComponent({
  props: {
    label: String,
    value: [String, Number],
    mono: Boolean,
    highlight: Boolean,
  },
  setup: (p) => () =>
    h(
      "div",
      { class: ["info-item", { mono: p.mono, highlight: p.highlight }] },
      [h("dt", p.label), h("dd", p.value ?? "—")],
    ),
});
const route = useRoute(),
  router = useRouter(),
  toast = useToastStore(),
  d = ref<ExtractionDetail | null>(null),
  confirming = ref(false),
  rejecting = ref(false),
  showReject = ref(false),
  rejectReason = ref("");
const navs = [
  { key: "tasks", label: "合同对比", icon: ListTodo, to: "/tasks" },
  {
    key: "extractions",
    label: "采购申请单",
    icon: ClipboardList,
    to: "/extractions",
  },
];
const title = computed(
    () => {
      const applicationTitle = usable(d.value?.applicationTitle);
      if (applicationTitle) return applicationTitle;
      const itemName = usable(d.value?.itemName);
      if (itemName) return `采购申请单-${itemName}`;
      return d.value?.applicationNo || "未命名申请单";
    },
  ),
  activeStep = computed(() =>
    d.value?.applicationStatus === "已确认"
      ? 3
      : d.value?.applicationStatus === "审批驳回"
        ? 1
        : 2,
  ),
  steps = [
    { label: "资料解析", detail: "字段已提取" },
    { label: "合同比对", detail: "规则已校验" },
    { label: "人工确认", detail: "当前阶段" },
    { label: "正式订单", detail: "确认后处理" },
  ],
  createdAt = computed(() => formatDateTime(d.value?.createTime)),
  unitPrice = computed(() =>
    d.value?.purchaseUnitPrice
      ? `${d.value.purchaseUnitPrice} ${d.value.currency || ""}`
      : "—",
  ),
  money = computed(() =>
    d.value?.taxInclusiveAmount
      ? `${d.value.taxInclusiveAmount} ${d.value.currency || ""}`
      : "—",
  );
function formatDateTime(value: string | null | undefined) {
  if (!value) return "—";
  const normalized = value.replace("T", " ");
  return normalized.length >= 16 ? normalized.slice(0, 16) : normalized;
}
function usable(value: string | null | undefined) {
  const normalized = value?.trim();
  return normalized && !/^\?+$/.test(normalized) ? normalized : "";
}
function clean(v: string | null | undefined) {
  return usable(v) || "待补充";
}
async function load() {
  const res: any = await extractionApi.get(Number(route.params.id));
  d.value = res.data;
}
async function onConfirm() {
  if (!d.value) return;
  confirming.value = true;
  try {
    const response: any = await extractionApi.confirm(d.value.id);
    const result = response.data;
    toast.success(
      result.orderNo
        ? `采购订单 ${result.orderNo} 已创建并提交`
        : result.message || "申请单已确认",
    );
    await load();
  } finally {
    confirming.value = false;
  }
}
async function onReject() {
  if (!d.value) return;
  rejecting.value = true;
  try {
    const res: any = await extractionApi.reject(d.value.id, rejectReason.value);
    toast.success("已驳回并创建补充请求");
    showReject.value = false;
    router.push(`/supplements/${res.data.id}`);
  } finally {
    rejecting.value = false;
  }
}
onMounted(load);
</script>
<style scoped>
.confirm-page {
  display: grid;
  gap: 28px;
}
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.hero-actions a {
  text-decoration: none;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.title-line h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 750;
  line-height: 1.25;
  letter-spacing: 0;
}
.title-line :deep(.status-badge) {
  padding: 8px 14px;
  font-size: 14px;
}
.hero-actions .action-btn {
  min-height: 46px;
  padding-inline: 18px;
  font-size: 15px;
}
.danger {
  color: #d33d3d !important;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}
.info-card {
  min-width: 0;
  padding: 28px;
  border: 1px solid #e2e6eb;
  border-radius: 16px;
  background: #fff;
  box-shadow: none;
}
.info-card.wide {
  grid-column: 1/-1;
}
.card-heading {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 24px;
  color: #4f8df7;
}
.card-heading h3 {
  margin: 0;
  color: #71829b;
  font-size: 17px;
  font-weight: 700;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 36px;
  row-gap: 24px;
  margin: 0;
}
.detail-grid.stacked {
  grid-template-columns: 1fr;
  gap: 20px;
}
.detail-grid.product-grid,
.detail-grid.supplier-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.detail-grid :deep(.info-item) {
  min-width: 0;
  padding: 0;
  background: transparent;
}
.detail-grid :deep(dt) {
  color: #8796ad;
  font-size: 15px;
  font-weight: 500;
}
.detail-grid :deep(dd) {
  margin: 8px 0 0;
  color: #111827;
  font-size: 17px;
  font-weight: 400;
  line-height: 1.55;
  overflow-wrap: anywhere;
}
.detail-grid :deep(.mono dd) {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 400;
}
.detail-grid :deep(.highlight) {
  background: transparent;
}
.detail-grid :deep(.highlight dd) {
  color: #4f8df7;
  font-size: 20px;
  font-weight: 400;
}
.note-card {
  display: flex;
  gap: 12px;
  padding: 22px 26px;
}
.note-card > svg {
  color: var(--primary);
}
.note-card strong {
  font-size: 16px;
}
.note-card p {
  margin: 6px 0 0;
  color: #526176;
  font-size: 15px;
  line-height: 1.65;
}
.detail-skeleton {
  display: grid;
  gap: 20px;
}
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  padding: 20px;
}
.modal-card {
  width: min(460px, 100%);
  padding: 24px;
  border-radius: 24px;
  background: white;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
}
.modal-icon {
  width: 44px;
  height: 44px;
  border-radius: 15px;
  display: grid;
  place-items: center;
  background: #fff0f0;
  color: #dc3f3f;
}
.modal-card h3 {
  margin: 15px 0 4px;
}
.modal-card > p {
  margin: 0;
  color: var(--muted-foreground);
  font-size: 12px;
}
.modal-card textarea {
  box-sizing: border-box;
  width: 100%;
  margin-top: 16px;
  padding: 13px;
  border: 1px solid var(--input);
  border-radius: 15px;
  font: inherit;
  outline: none;
}
.modal-card textarea:focus {
  border-color: #f29a9a;
  box-shadow: 0 0 0 4px rgba(220, 63, 63, 0.08);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.danger-fill {
  background: #dc3f3f !important;
  border-color: #dc3f3f !important;
}
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .modal-card,
.modal-leave-active .modal-card {
  transition: transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-card {
  transform: translateY(12px) scale(0.97);
}
@media (max-width: 800px) {
  .detail-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .detail-grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 520px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .modal-actions {
    flex-direction: column-reverse;
  }
  .modal-actions .action-btn {
    width: 100%;
  }
}
</style>
