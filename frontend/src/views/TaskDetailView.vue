<template>
  <AppShell :navs="navs" search-placeholder="搜索任务...">
    <div v-if="d" class="flex flex-col gap-5">
      <!-- Breadcrumb -->
      <div class="flex items-center gap-2 text-sm" style="color:var(--muted-foreground)">
        <router-link to="/tasks" class="flex items-center gap-1"><ChevronLeft :size="16" />任务列表</router-link>
        <span>/</span><span class="truncate font-medium" style="color:var(--foreground)">{{ d.title }}</span>
      </div>
      <!-- Title row -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div class="flex items-center gap-3 flex-wrap">
          <h2 class="text-xl font-bold truncate">{{ d.title }}</h2>
          <StatusBadge :status="d.status" />
          <RiskBadge :level="d.riskLevel" />
        </div>
        <div class="flex items-center gap-3">
          <button class="action-btn ghost" @click="onExport"><Download :size="16" /> 导出报告</button>
          <button v-if="d.status==='FAILED'" class="action-btn ghost" @click="onRetry">重跑</button>
          <button v-if="d.status==='DONE'" class="action-btn primary" @click="onConfirm">确认创建采购单</button>
        </div>
      </div>
      <!-- AI Risk Summary -->
      <article class="content-card" style="padding:calc(var(--spacing)*5)">
        <div class="flex items-center gap-2 mb-4">
          <Sparkles :size="18" style="color:var(--primary)" />
          <span class="text-xs font-semibold uppercase" style="letter-spacing:.08em;color:var(--muted-foreground)">AI 风险摘要</span>
        </div>
        <div class="summary-body" v-html="renderedSummary"></div>
      </article>
      <!-- Two-column comparison -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <ContractCard side="BUY" :ex="d.buy?.extraction" :filename="d.buy?.filename" />
        <ContractCard side="SELL" :ex="d.sell?.extraction" :filename="d.sell?.filename" />
      </div>
      <!-- Diff Table -->
      <article class="content-card" style="overflow:hidden">
        <div class="flex items-center gap-2 px-5 py-4" style="border-bottom:1px solid var(--border)">
          <Search :size="18" style="color:var(--primary)" /><span class="text-sm font-semibold">字段差异对比</span>
        </div>
        <FieldDiffTable :diffs="d.differences" />
      </article>
      <!-- Chat -->
      <article class="content-card" style="overflow:hidden">
        <div class="flex items-center gap-2 px-5 py-4" style="border-bottom:1px solid var(--border)">
          <MessageCircle :size="18" style="color:var(--primary)" /><span class="text-sm font-semibold">对话追问</span>
        </div>
        <ChatPanel :task-id="Number($route.params.id)" />
      </article>
    </div>
    <div v-else class="py-20 text-center" style="color:var(--muted-foreground)">加载中…</div>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronLeft, Download, Sparkles, Search, MessageCircle, ListTodo, ClipboardList } from 'lucide-vue-next'
import { marked } from 'marked'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import RiskBadge from '@/components/RiskBadge.vue'
import FieldDiffTable from '@/components/FieldDiffTable.vue'
import ChatPanel from '@/components/ChatPanel.vue'
import ContractCard from '@/components/ContractCard.vue'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const toast = useToastStore()
const d = ref<any>(null)
const navs = [
  { key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '申请单列表', icon: ClipboardList, to: '/extractions' }
]
const renderedSummary = computed(() => marked.parse(d.value?.summary || '暂无摘要'))
async function load() {
  const res: any = await taskApi.detail(Number(route.params.id))
  d.value = res.data
}
async function onExport() {
  const res: any = await taskApi.report(Number(route.params.id))
  const blob = new Blob([res.data || res], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${d.value.title}.md`
  a.click()
  URL.revokeObjectURL(url)
}
async function onRetry() { await taskApi.retry(Number(route.params.id)); toast.success('已重跑'); load() }
async function onConfirm() { await taskApi.confirm(Number(route.params.id)); toast.success('已确认'); load() }
onMounted(load)
</script>
<style scoped>
.summary-body { font-size: 0.9rem; line-height: 1.7; color: var(--foreground); }
.summary-body :deep(p) { margin: 0.4em 0; }
.summary-body :deep(p:first-child) { margin-top: 0; }
.summary-body :deep(p:last-child) { margin-bottom: 0; }
</style>
