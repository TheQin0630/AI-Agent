<template>
  <AppShell :navs="navs" search-placeholder="搜索任务、用户或日志...">
    <!-- Hero -->
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">任务审计</h2>
        <p>全量对比任务检索与追溯</p>
      </div>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="ctl">
        <label>状态</label>
        <select v-model="filter.status" class="ctl-select">
          <option value="">全部</option>
          <option v-for="s in statusOpts" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
      <div class="ctl">
        <label>风险等级</label>
        <select v-model="filter.riskLevel" class="ctl-select">
          <option value="">全部</option>
          <option v-for="r in riskOpts" :key="r" :value="r">{{ r }}</option>
        </select>
      </div>
      <div class="ctl">
        <label>用户 ID</label>
        <input type="number" v-model="filter.userId" placeholder="可选" class="ctl-input" />
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

    <!-- Task table card -->
    <article class="content-card">
      <div class="card-head">
        <div>
          <span class="eyebrow">执行面板</span>
          <strong class="block text-sm" style="margin-top:.2rem">任务列表</strong>
        </div>
        <span class="pill-secondary">{{ total }} 条</span>
      </div>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th style="min-width:80px">任务ID</th>
              <th style="min-width:200px">任务名</th>
              <th style="min-width:100px">用户</th>
              <th style="min-width:90px">状态</th>
              <th style="min-width:90px">风险</th>
              <th style="min-width:120px">创建时间</th>
              <th style="min-width:80px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in tasks" :key="t.id">
              <td class="cell-mono">#{{ t.id }}</td>
              <td class="cell-task-name truncate" style="max-width:260px">{{ t.title }}</td>
              <td class="whitespace-nowrap">{{ userCell(t) }}</td>
              <td><StatusBadge :status="t.status" /></td>
              <td><RiskBadge :level="t.riskLevel" /></td>
              <td class="cell-mono whitespace-nowrap">{{ fmt(t.createdAt) }}</td>
              <td>
                <div class="op-row">
                  <button class="op-btn" type="button" @click="onView(t)">
                    <Eye :size="13" style="margin-right:4px" /> 查看
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!tasks.length">
              <td colspan="7" class="text-center py-8" style="color:var(--muted-foreground)">暂无任务</td>
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
import { ListTodo, LayoutDashboard, Users, ShieldAlert, FileText, Search, Eye, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import RiskBadge from '@/components/RiskBadge.vue'
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

const statusOpts = ['PENDING', 'RUNNING', 'DONE', 'FAILED']
const riskOpts = ['LOW', 'MEDIUM', 'HIGH']
const filter = reactive({ status: '', riskLevel: '', userId: '' as string | number, from: '', to: '' })
const tasks = ref<any[]>([])
const total = ref(0)
const page = ref(1)

function fmt(s: string) { return s ? dayjs(s).format('YYYY-MM-DD HH:mm') : '—' }
function userCell(t: any) {
  if (t.username) return t.username
  if (t.userId) return '#' + t.userId
  return '—'
}

async function load() {
  const params: any = { page: page.value, size: 20 }
  if (filter.status) params.status = filter.status
  if (filter.riskLevel) params.riskLevel = filter.riskLevel
  if (filter.userId !== '' && filter.userId != null) params.userId = filter.userId
  if (filter.from) params.from = filter.from
  if (filter.to) params.to = filter.to
  try {
    const res: any = await adminApi.listTasks(params)
    tasks.value = res.data.items || []
    total.value = res.data.total || 0
  } catch { /* toast 已由拦截器处理 */ }
}
function onSearch() { page.value = 1; load() }

// MVP：管理端无独立任务详情路由，复用 frontend 用户端 TaskDetailView，新窗口打开
function onView(t: any) {
  window.open(`http://localhost:5173/tasks/${t.id}`, '_blank')
}

onMounted(load)
</script>
<style scoped>
.filter-bar {
  display: flex; flex-wrap: wrap; align-items: flex-end; gap: calc(var(--spacing) * 3);
}
.ctl { display: flex; flex-direction: column; gap: calc(var(--spacing) * 2); }
.ctl label { font-size: .72rem; font-weight: 500; color: var(--muted-foreground); }
.ctl-select, .ctl-input {
  height: 32px; padding: 0 calc(var(--spacing) * 3);
  border: 1px solid var(--input); border-radius: var(--radius);
  background: var(--popover); color: var(--foreground); font: inherit; font-size: .85rem;
  outline: none; min-width: 120px;
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
.op-row { display: flex; align-items: center; justify-content: flex-end; gap: calc(var(--spacing) * 2); }
.op-btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: calc(var(--spacing) * 2) calc(var(--spacing) * 3);
  border-radius: var(--radius); border: 1px solid var(--border);
  background: transparent; color: var(--foreground);
  font: 500 0.75rem/1 var(--font-sans); cursor: pointer;
  transition: background 0.15s ease;
}
.op-btn:hover { background: var(--accent); color: var(--accent-foreground); }
</style>
