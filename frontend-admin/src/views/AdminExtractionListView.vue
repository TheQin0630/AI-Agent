<template>
  <AppShell :navs="navs" search-placeholder="搜索申请单编号、供应商、商品...">
    <!-- Hero（对齐 AdminTaskListView） -->
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">采购申请单审计</h2>
        <p>Dify Agent 自动创建的采购申请单审核与确认</p>
      </div>
    </div>

    <!-- Filter bar（对齐 AdminTaskListView 的 filter-bar） -->
    <div class="filter-bar">
      <div class="ctl">
        <label>状态</label>
        <select v-model="filter.status" class="ctl-select">
          <option value="">全部</option>
          <option v-for="s in statusOpts" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
      <button class="action-btn primary" type="button" @click="onSearch">
        <Search :size="15" /> 查询
      </button>
    </div>

    <!-- 申请单表格卡片（对齐 AdminTaskListView 的 content-card） -->
    <article class="content-card">
      <div class="card-head">
        <div>
          <span class="eyebrow">执行面板</span>
          <strong class="block text-sm" style="margin-top:.2rem">申请单列表</strong>
        </div>
        <span class="pill-secondary">{{ total }} 条</span>
      </div>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th style="min-width:80px">ID</th>
              <th style="min-width:160px">申请单编号</th>
              <th style="min-width:180px">标题</th>
              <th style="min-width:90px">状态</th>
              <th style="min-width:140px">供应商</th>
              <th style="min-width:120px">商品</th>
              <th style="min-width:120px">创建时间</th>
              <th style="min-width:80px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in items" :key="e.id">
              <td class="cell-mono">#{{ e.id }}</td>
              <td class="cell-mono whitespace-nowrap">{{ e.applicationNo || '—' }}</td>
              <td class="truncate" style="max-width:220px">{{ e.applicationTitle || '—' }}</td>
              <td><ExtractionStatusBadge :status="e.applicationStatus" /></td>
              <td class="truncate" style="max-width:180px">{{ e.supplierName || '—' }}</td>
              <td class="truncate" style="max-width:160px">{{ e.itemName || '—' }}</td>
              <td class="cell-mono whitespace-nowrap">{{ fmt(e.createTime) }}</td>
              <td>
                <div class="op-row">
                  <button class="op-btn" type="button" @click="onView(e)">
                    <Eye :size="13" style="margin-right:4px" /> 查看
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!items.length">
              <td colspan="8" class="text-center py-8" style="color:var(--muted-foreground)">暂无申请单</td>
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
import { useRouter } from 'vue-router'
import { ListTodo, LayoutDashboard, Users, ShieldAlert, FileText, Search, Eye, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import ExtractionStatusBadge from '@/components/ExtractionStatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import { extractionApi, type ExtractionListItem } from '@/api/extraction'
import dayjs from 'dayjs'

// 与管理端其他页面一致的 navs（追加"申请单"入口）
const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '采购申请单', icon: ClipboardList, to: '/extractions' },
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]

const router = useRouter()
const statusOpts = ['待确认', '已确认', '待提交', '创建失败']
const filter = reactive({ status: '' })
const items = ref<ExtractionListItem[]>([])
const total = ref(0)
const page = ref(1)

function fmt(s: string | null) { return s ? dayjs(s).format('YYYY-MM-DD HH:mm') : '—' }

async function load() {
  const params: any = { page: page.value, size: 20 }
  if (filter.status) params.status = filter.status
  try {
    const res: any = await extractionApi.list(params)
    items.value = res.data.items || []
    total.value = res.data.total || 0
  } catch { /* toast 已由拦截器处理 */ }
}
function onSearch() { page.value = 1; load() }
function onView(e: ExtractionListItem) {
  router.push(`/extractions/${e.id}`)
}

onMounted(load)
</script>
<style scoped>
/* 对齐 AdminTaskListView 的样式 */
.filter-bar {
  display: flex; flex-wrap: wrap; align-items: flex-end; gap: calc(var(--spacing) * 3);
}
.ctl { display: flex; flex-direction: column; gap: calc(var(--spacing) * 2); }
.ctl label { font-size: .72rem; font-weight: 500; color: var(--muted-foreground); }
.ctl-select {
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
