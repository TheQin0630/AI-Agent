<template>
  <AppShell :navs="navs" search-placeholder="搜索申请单...">
    <div v-if="d" class="flex flex-col gap-5">
      <!-- Breadcrumb -->
      <div class="flex items-center gap-2 text-sm" style="color:var(--muted-foreground)">
        <router-link to="/extractions" class="flex items-center gap-1"><ChevronLeft :size="16" />申请单列表</router-link>
        <span>/</span><span class="truncate font-medium" style="color:var(--foreground)">{{ d.applicationTitle || d.applicationNo || '申请单详情' }}</span>
      </div>

      <!-- Title row（对齐管理端风格：action-btn primary/ghost） -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div class="flex items-center gap-3 flex-wrap">
          <h2 class="text-xl font-bold truncate">{{ d.applicationTitle || d.applicationNo || '未命名申请单' }}</h2>
          <ExtractionStatusBadge :status="d.applicationStatus" />
        </div>
        <div class="flex items-center gap-3">
          <button v-if="d.applicationStatus === '待确认'" class="action-btn primary" :disabled="confirming" @click="onConfirm">
            <Check :size="16" /> {{ confirming ? '确认中…' : '确认申请单' }}
          </button>
          <router-link v-if="d.applicationStatus === '已确认'" to="/extractions" class="action-btn ghost">
            <ChevronLeft :size="16" /> 返回列表
          </router-link>
        </div>
      </div>

      <!-- 申请单信息卡片（对齐管理端 content-card 结构） -->
      <article class="content-card" style="padding:calc(var(--spacing)*5)">
        <div class="flex items-center gap-2 mb-4">
          <FileText :size="18" style="color:var(--primary)" />
          <span class="eyebrow">申请单信息</span>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div class="meta-cell"><span class="meta-label">申请单编号</span><span class="meta-value mono">{{ d.applicationNo || '—' }}</span></div>
          <div class="meta-cell"><span class="meta-label">申请单标题</span><span class="meta-value">{{ d.applicationTitle || '—' }}</span></div>
          <div class="meta-cell"><span class="meta-label">申请类型</span><span class="meta-value">{{ d.applicationType || '—' }}</span></div>
          <div class="meta-cell"><span class="meta-label">申请日期</span><span class="meta-value">{{ d.applyDate || '—' }}</span></div>
          <div class="meta-cell"><span class="meta-label">币种</span><span class="meta-value">{{ d.currency || '—' }}</span></div>
          <div class="meta-cell"><span class="meta-label">创建时间</span><span class="meta-value">{{ fmt(d.createTime) }}</span></div>
        </div>
      </article>

      <!-- 双列：供应商信息 + 商品信息 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <article class="content-card" style="padding:calc(var(--spacing)*5)">
          <div class="flex items-center gap-2 mb-4">
            <Package :size="18" style="color:var(--primary)" />
            <span class="eyebrow">供应商信息</span>
          </div>
          <div class="flex flex-col gap-3">
            <div class="meta-cell"><span class="meta-label">供应商名称</span><span class="meta-value">{{ d.supplierName || '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">付款条款</span><span class="meta-value">{{ d.paymentTerms || '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">交付地点</span><span class="meta-value">{{ d.deliveryLocation || '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">预计交付日期</span><span class="meta-value">{{ d.expectedDeliveryDate || '—' }}</span></div>
          </div>
        </article>
        <article class="content-card" style="padding:calc(var(--spacing)*5)">
          <div class="flex items-center gap-2 mb-4">
            <ShoppingCart :size="18" style="color:var(--primary)" />
            <span class="eyebrow">商品信息</span>
          </div>
          <div class="flex flex-col gap-3">
            <div class="meta-cell"><span class="meta-label">商品名称</span><span class="meta-value">{{ d.itemName || '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">规格型号</span><span class="meta-value">{{ d.itemModel || '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">单位</span><span class="meta-value">{{ d.unit || '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">数量</span><span class="meta-value">{{ d.quantity ?? '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">采购单价</span><span class="meta-value">{{ d.purchaseUnitPrice ?? '—' }}</span></div>
            <div class="meta-cell"><span class="meta-label">采购总金额</span><span class="meta-value emphasize">{{ d.purchaseTotalAmount ?? '—' }} {{ d.currency || '' }}</span></div>
          </div>
        </article>
      </div>

      <!-- 备注信息 -->
      <article v-if="d.message" class="content-card" style="overflow:hidden">
        <div class="flex items-center gap-2 px-5 py-4" style="border-bottom:1px solid var(--border)">
          <MessageSquare :size="18" style="color:var(--primary)" /><span class="text-sm font-semibold">备注信息</span>
        </div>
        <div style="padding:calc(var(--spacing)*5)">
          <p class="text-sm" style="color:var(--foreground)">{{ d.message }}</p>
        </div>
      </article>
    </div>
    <div v-else class="py-20 text-center" style="color:var(--muted-foreground)">加载中…</div>
  </AppShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronLeft, Check, FileText, Package, ShoppingCart, MessageSquare, ListTodo, LayoutDashboard, Users, ShieldAlert, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import ExtractionStatusBadge from '@/components/ExtractionStatusBadge.vue'
import { extractionApi, type ExtractionDetail } from '@/api/extraction'
import { useToastStore } from '@/stores/toast'
import dayjs from 'dayjs'

const route = useRoute()
const toast = useToastStore()
const d = ref<ExtractionDetail | null>(null)
const confirming = ref(false)

// 与管理端其他页面一致的 navs
const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '采购申请单', icon: ClipboardList, to: '/extractions' },
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]

function fmt(s: string | null) {
  return s ? dayjs(s).format('YYYY-MM-DD HH:mm') : '—'
}

async function load() {
  try {
    const res: any = await extractionApi.get(Number(route.params.id))
    d.value = res.data
  } catch {
    toast.error('加载申请单失败')
  }
}

async function onConfirm() {
  if (!d.value) return
  confirming.value = true
  try {
    await extractionApi.confirm(d.value.id)
    toast.success('申请单已确认')
    await load()
  } catch {
    // 错误已由 http 拦截器提示
  } finally {
    confirming.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.eyebrow { font-size: .78rem; text-transform: uppercase; letter-spacing: .08em; font-weight: 600; color: var(--muted-foreground); }

.meta-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.meta-label {
  font-size: 12px;
  color: var(--muted-foreground);
  white-space: nowrap;
}
.meta-value {
  font-size: 14px;
  color: var(--foreground);
  word-break: break-word;
  font-weight: 500;
}
.meta-value.mono {
  font-family: var(--font-mono);
  font-weight: 400;
}
.meta-value.emphasize {
  color: var(--chart-1);
  font-weight: 700;
  font-size: 16px;
}
</style>
