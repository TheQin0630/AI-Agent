<template>
  <AppShell :navs="navs" search-placeholder="搜索任务、用户或日志...">
    <!-- Hero -->
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">LLM 调用日志</h2>
        <p>检索大模型调用记录、耗时与异常</p>
      </div>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="ctl">
        <label>用户 ID</label>
        <input type="number" v-model="filter.userId" placeholder="可选" class="ctl-input" />
      </div>
      <div class="ctl">
        <label>模型</label>
        <input type="text" v-model="filter.model" placeholder="如 gpt-4o-mini" class="ctl-input" />
      </div>
      <div class="ctl">
        <label>开始日期</label>
        <input type="date" v-model="filter.from" class="ctl-input" />
      </div>
      <div class="ctl">
        <label>结束日期</label>
        <input type="date" v-model="filter.to" class="ctl-input" />
      </div>
      <button class="action-btn primary" type="button" @click="onSearch">
        <Search :size="15" /> 查询
      </button>
    </div>

    <!-- Log table card -->
    <article class="content-card">
      <div class="card-head">
        <div>
          <span class="eyebrow">调用记录</span>
          <strong class="block text-sm" style="margin-top:.2rem">日志列表</strong>
        </div>
        <span class="pill-secondary">{{ total }} 条</span>
      </div>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th style="min-width:70px">ID</th>
              <th style="min-width:80px">任务ID</th>
              <th style="min-width:80px">用户ID</th>
              <th style="min-width:140px">模型</th>
              <th style="min-width:90px">Tokens</th>
              <th style="min-width:90px">耗时(ms)</th>
              <th style="min-width:70px">状态</th>
              <th style="min-width:160px">错误</th>
              <th style="min-width:140px">时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in logs" :key="l.id">
              <td class="cell-mono">{{ l.id }}</td>
              <td class="cell-mono">#{{ l.taskId }}</td>
              <td class="cell-mono">{{ l.userId }}</td>
              <td class="whitespace-nowrap">{{ l.model }}</td>
              <td class="cell-mono">{{ l.totalTokens ?? '—' }}</td>
              <td class="cell-mono">{{ l.durationMs ?? '—' }}</td>
              <td><span class="log-badge" :class="l.status === 'OK' ? 'st-ok' : 'st-fail'">{{ l.status }}</span></td>
              <td class="truncate" style="max-width:220px">{{ l.error || '—' }}</td>
              <td class="cell-mono whitespace-nowrap">{{ fmt(l.createdAt) }}</td>
            </tr>
            <tr v-if="!logs.length">
              <td colspan="9" class="text-center py-8" style="color:var(--muted-foreground)">暂无日志</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination :total="total" :page="page" :size="20" @update:page="page=$event; load()" />
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ListTodo, LayoutDashboard, Users, ShieldAlert, FileText, Search, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import Pagination from '@/components/Pagination.vue'
import { adminApi } from '@/api/admin'
import dayjs from 'dayjs'

const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '采购申请单', icon: ClipboardList, to: '/extractions' },
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]

const filter = reactive({ userId: '' as string | number, model: '', from: '', to: '' })
const logs = ref<any[]>([])
const total = ref(0)
const page = ref(1)

function fmt(s: string) { return s ? dayjs(s).format('YYYY-MM-DD HH:mm') : '—' }

async function load() {
  const params: any = { page: page.value, size: 20 }
  if (filter.userId !== '' && filter.userId != null) params.userId = filter.userId
  if (filter.model) params.model = filter.model
  if (filter.from) params.from = filter.from
  if (filter.to) params.to = filter.to
  try {
    const res: any = await adminApi.listLlmLogs(params)
    logs.value = res.data.items || []
    total.value = res.data.total || 0
  } catch { /* toast 已由拦截器处理 */ }
}
function onSearch() { page.value = 1; load() }

onMounted(load)
</script>
<style scoped>
.filter-bar {
  display: flex; flex-wrap: wrap; align-items: flex-end; gap: calc(var(--spacing) * 3);
}
.ctl { display: flex; flex-direction: column; gap: calc(var(--spacing) * 2); }
.ctl label { font-size: .72rem; font-weight: 500; color: var(--muted-foreground); }
.ctl-input {
  height: 32px; padding: 0 calc(var(--spacing) * 3);
  border: 1px solid var(--input); border-radius: var(--radius);
  background: var(--popover); color: var(--foreground); font: inherit; font-size: .85rem;
  outline: none; min-width: 140px;
}
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: calc(var(--spacing) * 4) calc(var(--spacing) * 5);
  border-bottom: 1px solid var(--border);
}
.eyebrow { font-size: .78rem; text-transform: uppercase; letter-spacing: .08em; font-weight: 600; color: var(--muted-foreground); }
.pill-secondary {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .34rem .65rem; border-radius: 999px;
  background: var(--secondary); color: var(--secondary-foreground); font-size: .8rem; font-weight: 500;
}
.log-badge {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .34rem .65rem; border-radius: 999px; font-size: .8rem; font-weight: 500; white-space: nowrap;
}
.st-ok { background: color-mix(in srgb, var(--chart-5) 18%, var(--background)); color: var(--chart-5); }
.st-fail { background: color-mix(in srgb, var(--chart-2) 18%, var(--background)); color: var(--chart-2); }
</style>
