<template>
  <AppShell :navs="navs" search-placeholder="搜索任务名称、合同编号...">
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">我的对比任务</h2>
        <p>创建和管理采购合同与销售合同的智能对比分析</p>
      </div>
      <div class="hero-actions">
        <button class="action-btn primary" @click="$router.push('/tasks/new')">
          <Plus :size="16" /> 新建对比
        </button>
        <button class="action-btn ghost" @click="onExport">
          <Download :size="16" /> 导出报告
        </button>
      </div>
    </div>
    <article class="content-card">
      <FilterChips :options="filters" v-model="activeFilter" />
      <table class="data-table">
        <thead>
          <tr>
            <th style="min-width:160px">任务名称</th>
            <th style="min-width:90px">状态</th>
            <th style="min-width:90px">风险等级</th>
            <th style="min-width:110px">创建时间</th>
            <th style="min-width:110px">更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.id" @click="$router.push(`/tasks/${t.id}`)" style="cursor:pointer">
            <td class="cell-task-name truncate" style="max-width:220px">{{ t.title }}</td>
            <td><StatusBadge :status="t.status" /></td>
            <td><RiskBadge :level="t.riskLevel" /></td>
            <td class="cell-mono whitespace-nowrap">{{ fmt(t.createdAt) }}</td>
            <td class="cell-mono whitespace-nowrap">{{ fmt(t.updatedAt) }}</td>
          </tr>
          <tr v-if="!tasks.length">
            <td colspan="5" class="text-center py-8" style="color:var(--muted-foreground)">暂无任务</td>
          </tr>
        </tbody>
      </table>
      <Pagination :total="total" :page="page" :size="20" @update:page="page=$event; load()" />
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Plus, Download, ListTodo, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import FilterChips from '@/components/FilterChips.vue'
import Pagination from '@/components/Pagination.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import RiskBadge from '@/components/RiskBadge.vue'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'
import dayjs from 'dayjs'

const navs = [
  { key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '申请单列表', icon: ClipboardList, to: '/extractions' }
]
const filters = ['全部', '已完成', '进行中', '待处理', '失败']
const activeFilter = ref('全部')
const tasks = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const toast = useToastStore()
const statusMap: Record<string, string|undefined> = {
  '全部': undefined, '已完成': 'DONE', '进行中': 'RUNNING', '待处理': 'PENDING', '失败': 'FAILED'
}
function fmt(s: string) { return s ? dayjs(s).format('YYYY-MM-DD') : '—' }
async function load() {
  const res: any = await taskApi.list({ page: page.value, size: 20, status: statusMap[activeFilter.value] })
  tasks.value = res.data.items
  total.value = res.data.total
}
function onExport() { toast.info('请进入任务详情页导出报告') }
watch(activeFilter, () => { page.value = 1; load() })
onMounted(load)
</script>
