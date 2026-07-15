<template>
  <AppShell :navs="navs" search-placeholder="搜索任务、用户或日志...">
    <!-- Hero -->
    <div>
      <h2 style="margin:0;font-size:1.7rem;text-wrap:balance;word-break:keep-all;overflow-wrap:break-word">管理概览</h2>
      <p style="margin:.45rem 0 0;color:var(--muted-foreground);font-size:.95rem">系统运行状态与关键指标监控</p>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid">
      <article class="stat-card" v-for="s in stats" :key="s.label">
        <span class="stat-eyebrow">{{ s.label }}</span>
        <div class="stat-value">{{ s.value }}</div>
        <span class="stat-badge" :class="s.badgeCls">{{ s.badge }}</span>
      </article>
    </div>

    <!-- Two-Column Grid: Chart + Activity -->
    <div class="panels-grid">
      <!-- Left: Task Status Distribution Bar Chart -->
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <div class="stat-eyebrow">任务状态分布</div>
            <strong style="font-size:.95rem">全部 {{ taskTotal }} 项任务</strong>
          </div>
          <span class="pill-muted">实时</span>
        </div>
        <!-- CSS Bar Chart -->
        <div class="bar-chart">
          <div class="bar-col" v-for="b in bars" :key="b.key">
            <span class="bar-value">{{ b.count }}</span>
            <div class="bar"
              :style="`width:100%;max-width:56px;border-radius:999px 999px 10px 10px;background:${b.color};height:${b.height}%;min-height:20px;transition:height .3s ease`"></div>
          </div>
        </div>
        <div class="bar-labels">
          <span class="bar-label" v-for="b in bars" :key="b.key">{{ b.key }}</span>
        </div>
      </article>

      <!-- Right: Recent Activity List -->
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <div class="stat-eyebrow">动态</div>
            <strong style="font-size:.95rem">最近活动</strong>
          </div>
          <span class="pill-secondary">{{ activities.length }} 条</span>
        </div>
        <div class="activity-list">
          <!-- TODO: 后端暂无活动流接口，以下为前端静态文案聚合，后续接入活动流 API -->
          <div class="activity-item" v-for="(a, i) in activities" :key="i">
            <div class="activity-text">{{ a.text }}</div>
            <span class="activity-time">{{ a.time }}</span>
          </div>
        </div>
      </article>
    </div>

    <!-- Bottom: Recent Tasks Table -->
    <article class="content-card">
      <div class="recent-head">
        <div>
          <span class="stat-eyebrow">执行面板</span>
          <strong style="font-size:.95rem;margin-left:calc(var(--spacing)*3)">最近任务</strong>
        </div>
        <button class="csv-btn" type="button" @click="onExport">导出 CSV</button>
      </div>
      <div class="task-table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>任务ID</th>
              <th>任务名称</th>
              <th>用户</th>
              <th>状态</th>
              <th>风险等级</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in recentTasks" :key="t.id">
              <td class="cell-mono">#{{ t.id }}</td>
              <td class="cell-task-name truncate" style="max-width:260px">{{ t.title }}</td>
              <td class="whitespace-nowrap">{{ userCell(t) }}</td>
              <td><StatusBadge :status="t.status" /></td>
              <td><RiskBadge :level="t.riskLevel" /></td>
              <td class="cell-mono whitespace-nowrap">{{ fmt(t.createdAt) }}</td>
            </tr>
            <tr v-if="!recentTasks.length">
              <td colspan="6" class="text-center py-8" style="color:var(--muted-foreground)">暂无任务</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ListTodo, LayoutDashboard, Users, ShieldAlert, FileText, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import RiskBadge from '@/components/RiskBadge.vue'
import { adminApi } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import dayjs from 'dayjs'

const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '采购申请单', icon: ClipboardList, to: '/extractions' },
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]

const toast = useToastStore()
const userCount = ref(0)
const taskCount = ref(0)
const todayLlmCalls = ref(0)
const todayTokens = ref(0)
const tasksByStatus = ref<Record<string, number>>({})
const recentTasks = ref<any[]>([])

const taskTotal = computed(() => taskCount.value || 0)

const stats = computed(() => [
  { label: '注册用户', value: userCount.value, badge: '累计用户', badgeCls: 'badge-muted' },
  { label: '对比任务', value: taskCount.value, badge: '累计任务', badgeCls: 'badge-muted' },
  { label: '今日 LLM 调用', value: todayLlmCalls.value, badge: '正常', badgeCls: 'badge-green' },
  { label: '今日 Token 消耗', value: todayTokens.value.toLocaleString(), badge: '今日', badgeCls: 'badge-green' }
])

const bars = computed(() => {
  const s = tasksByStatus.value
  const items = [
    { key: 'PENDING', color: 'var(--chart-1)' },
    { key: 'RUNNING', color: 'var(--chart-3)' },
    { key: 'DONE', color: 'var(--chart-5)' },
    { key: 'FAILED', color: 'var(--chart-2)' }
  ]
  const max = Math.max(1, ...items.map(i => s[i.key] || 0))
  return items.map(i => ({ ...i, count: s[i.key] || 0, height: ((s[i.key] || 0) / max) * 100 }))
})

// TODO: 后端暂无活动流接口，暂用静态文案聚合（任务状态变化 + 用户注册）
const activities = ref([
  { text: '用户 zhangsan 创建了新对比任务', time: '2分钟前' },
  { text: '任务 #998 对比完成，风险等级：HIGH', time: '15分钟前' },
  { text: '用户 lisi 注册账号', time: '1小时前' },
  { text: '任务 #995 LLM 调用失败，已自动重试', time: '2小时前' },
  { text: '管理员修改了风险规则 #3', time: '3小时前' }
])

function fmt(s: string) { return s ? dayjs(s).format('YYYY-MM-DD HH:mm') : '—' }
function userCell(t: any) {
  if (t.username) return t.username
  if (t.userId) return '#' + t.userId
  return '—'
}

async function load() {
  try {
    const res: any = await adminApi.stats()
    const d = res.data
    userCount.value = d.userCount || 0
    taskCount.value = d.taskCount || 0
    todayLlmCalls.value = d.todayLlmCalls || 0
    todayTokens.value = d.todayTokens || 0
    tasksByStatus.value = d.tasksByStatus || {}
  } catch { /* toast 已由拦截器处理 */ }
  try {
    const r: any = await adminApi.listTasks({ page: 1, size: 5, sort: 'createdAt,desc' })
    recentTasks.value = r.data.items || []
  } catch { /* 忽略，表格显示暂无任务 */ }
}

function onExport() {
  if (!recentTasks.value.length) return toast.info('暂无数据可导出')
  const header = ['任务ID', '任务名称', '用户', '状态', '风险等级', '创建时间']
  const rows = recentTasks.value.map(t => [t.id, t.title, userCell(t), t.status, t.riskLevel || '', fmt(t.createdAt)])
  const csv = [header, ...rows].map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'recent-tasks.csv'; a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>
<style scoped>
/* 拷贝自 design/pages/管理概览.html 的 stats-grid / panels-grid / 柱状图 / 活动列表 样式 */
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: calc(var(--spacing) * 4); }
.stat-card {
  background: var(--card); color: var(--card-foreground);
  border: 1px solid var(--border); border-radius: calc(var(--radius) + 6px);
  padding: calc(var(--spacing) * 5); box-shadow: var(--shadow-sm);
}
.stat-eyebrow { font-size: .78rem; text-transform: uppercase; letter-spacing: .08em; color: var(--muted-foreground); }
.stat-value { font-size: 1.8rem; font-weight: 700; margin: .45rem 0; white-space: nowrap; }
.stat-badge {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .3rem .6rem; border-radius: 999px; font-size: .82rem;
}
.badge-muted { background: var(--muted); color: var(--foreground); }
.badge-green { background: color-mix(in srgb, var(--chart-5) 18%, var(--background)); color: var(--chart-5); font-weight: 600; }

.panels-grid { display: grid; grid-template-columns: 1.7fr 1fr; gap: calc(var(--spacing) * 4); }
.panel-card {
  background: var(--card); color: var(--card-foreground);
  border: 1px solid var(--border); border-radius: calc(var(--radius) + 6px);
  padding: calc(var(--spacing) * 5); box-shadow: var(--shadow-sm);
}
.panel-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: calc(var(--spacing) * 3); margin-bottom: calc(var(--spacing) * 4);
}
.pill-muted {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .34rem .65rem; border-radius: 999px;
  background: var(--muted); color: var(--foreground); font-size: .8rem;
}
.pill-secondary {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .34rem .65rem; border-radius: 999px;
  background: var(--secondary); color: var(--secondary-foreground); font-size: .8rem;
}
.bar-chart { display: flex; align-items: flex-end; gap: calc(var(--spacing) * 5); height: 180px; padding-top: calc(var(--spacing) * 2); }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: calc(var(--spacing) * 2); height: 100%; }
.bar-value { font-size: .78rem; font-weight: 600; color: var(--foreground); }
.bar-labels { display: flex; gap: calc(var(--spacing) * 5); margin-top: calc(var(--spacing) * 3); }
.bar-label { flex: 1; text-align: center; font-size: .78rem; color: var(--muted-foreground); white-space: nowrap; }

.activity-list { display: flex; flex-direction: column; gap: 0; }
.activity-item {
  display: flex; justify-content: space-between; gap: calc(var(--spacing) * 3);
  padding: calc(var(--spacing) * 3.5) 0; border-top: 1px solid var(--border);
}
.activity-text { min-width: 0; font-size: .88rem; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.activity-time { font-size: .78rem; color: var(--muted-foreground); white-space: nowrap; flex-shrink: 0; }

.recent-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
  border-bottom: 1px solid var(--border);
}
.csv-btn {
  border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--background); color: var(--foreground);
  padding: 6px 10px; font: inherit; cursor: pointer; white-space: nowrap;
}
.task-table-wrap { overflow-x: auto; }

@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: 1fr 1fr; }
  .panels-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .stats-grid { grid-template-columns: 1fr 1fr; }
}
</style>
