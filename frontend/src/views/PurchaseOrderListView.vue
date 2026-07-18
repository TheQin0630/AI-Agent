<template>
  <AppShell :navs="navs" search-placeholder="搜索订单编号、申请单编号、供应商或商品" v-model:search="search">
    <header class="page-hero"><div><p class="section-kicker">Purchase Orders</p><h2>采购订单</h2><p>集中查看审批通过并完成库存对比后生成的正式采购订单。</p></div></header>
    <section class="summary">
      <span class="summary-icon"><ShoppingCart :size="22" /></span>
      <div><small>正式订单</small><strong>{{ total }} <em>单</em></strong></div>
      <p><CircleCheckBig :size="17" />列表中的订单均已正式提交</p>
    </section>
    <article class="content-card list-card">
      <div class="list-toolbar"><div><h3>订单明细</h3><p>点击订单查看申请来源、供应商及商品信息</p></div></div>
      <div class="table-scroll"><table class="data-table">
        <thead><tr><th>采购订单编号</th><th>对应申请单</th><th>供应商 / 商品</th><th>含税采购金额</th><th>状态</th><th>创建时间</th><th /></tr></thead>
        <tbody v-if="loading"><tr v-for="n in 5" :key="n"><td v-for="m in 7" :key="m"><div class="skeleton cell-skeleton" /></td></tr></tbody>
        <tbody v-else>
          <tr v-for="order in visibleItems" :key="order.id" class="data-row" @click="open(order.id)">
            <td class="cell-mono">{{ order.orderNo }}</td><td class="cell-mono">{{ order.applicationNo || "—" }}</td>
            <td><div class="primary-cell"><strong>{{ order.supplierName }}</strong><span>{{ order.itemName }}</span></div></td>
            <td class="amount">{{ money(order.totalAmount, order.currency) }}</td>
            <td><span class="status-pill"><i />已提交</span></td><td class="cell-mono">{{ formatTime(order.createdAt) }}</td>
            <td><button class="row-action" aria-label="查看订单"><ChevronRight :size="17" /></button></td>
          </tr>
          <tr v-if="!visibleItems.length"><td colspan="7"><div class="empty-state"><ShoppingCart :size="30" /><strong>暂无采购订单</strong><span>正式订单生成后会显示在这里</span></div></td></tr>
        </tbody>
      </table></div>
      <Pagination :total="total" :page="page" :size="20" @update:page="changePage" />
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ChevronRight, CircleCheckBig, ListTodo, ShoppingCart } from "lucide-vue-next";
import AppShell from "@/components/AppShell.vue";
import Pagination from "@/components/Pagination.vue";
import { purchaseOrderApi, type PurchaseOrderListItem } from "@/api/purchaseOrder";
const router = useRouter(), navs = [{ key: "tasks", label: "合同对比", icon: ListTodo, to: "/tasks" }];
const items = ref<PurchaseOrderListItem[]>([]), total = ref(0), page = ref(1), loading = ref(true), search = ref("");
const visibleItems = computed(() => { const q = search.value.trim().toLowerCase(); return q ? items.value.filter(i => [i.orderNo, i.applicationNo, i.supplierName, i.itemName].some(v => String(v || "").toLowerCase().includes(q))) : items.value; });
async function load() { loading.value = true; try { const r: any = await purchaseOrderApi.list({ page: page.value, size: 20 }); items.value = r.data.items; total.value = r.data.total; } finally { loading.value = false; } }
function changePage(v: number) { page.value = v; void load(); }
function open(id: number) { router.push(`/purchase-orders/${id}`); }
function money(v: string, c: string) { return `${Number(v || 0).toLocaleString("zh-CN", { minimumFractionDigits: 2 })} ${c || ""}`; }
function formatTime(v: string) { return v?.replace("T", " ").slice(0, 16) || "—"; }
onMounted(load);
</script>
<style scoped>
.list-card {
  font-family: "Microsoft YaHei", "PingFang SC", Inter, sans-serif;
}
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px 18px;
  border-bottom: 1px solid var(--border);
}
.list-toolbar h3 {
  margin: 0;
  color: var(--foreground);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
}
.list-toolbar p {
  margin: 4px 0 0;
  color: var(--muted-foreground);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.5;
}
.page-hero h2{margin:4px 0 8px;font-size:32px}.page-hero p:last-child{margin:0;color:var(--muted-foreground)}
.summary{display:flex;align-items:center;gap:14px;padding:22px 26px;border:1px solid #d9e7fb;border-radius:20px;background:linear-gradient(135deg,#f5f9ff,#fff);box-shadow:0 10px 30px rgba(40,91,156,.06)}
.summary-icon{width:46px;height:46px;display:grid;place-items:center;border-radius:15px;background:#e6f0ff;color:#1769e0}.summary small{display:block;color:#66758b}.summary strong{display:block;font-size:24px}.summary em{font-size:13px;font-style:normal;color:#718096}.summary p{display:flex;align-items:center;gap:7px;margin-left:auto;color:#24815e;font-size:13px}.amount{color:#1769e0;font-weight:650;white-space:nowrap}.status-pill{display:inline-flex;align-items:center;gap:7px;padding:6px 10px;border-radius:999px;background:#e9f8f1;color:#16805b;font-size:12px}.status-pill i{width:6px;height:6px;border-radius:50%;background:#19a974}.cell-skeleton{width:82px;height:15px}.data-row{cursor:pointer}
.empty-state {
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--muted-foreground);
  text-align: center;
}
.empty-state svg {
  margin-bottom: 4px;
  color: #7d91aa;
}
.empty-state strong {
  color: var(--foreground);
  font-size: 15px;
  font-weight: 600;
}
.empty-state span {
  font-size: 13px;
  font-weight: 400;
}
@media(max-width:720px){.summary{align-items:flex-start;flex-wrap:wrap}.summary p{margin-left:0;width:100%}.list-toolbar{padding:18px}}
</style>
