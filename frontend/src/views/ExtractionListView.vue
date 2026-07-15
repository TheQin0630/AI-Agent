<template>
  <AppShell :navs="navs" search-placeholder="搜索申请单编号、供应商、商品...">
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">采购申请单</h2>
        <p>查看 Dify Agent 自动创建的采购申请单，进行人工审核确认</p>
      </div>
    </div>
    <article class="content-card">
      <FilterChips :options="filters" v-model="activeFilter" />
      <table class="data-table">
        <thead>
          <tr>
            <th style="min-width:160px">申请单编号</th>
            <th style="min-width:160px">标题</th>
            <th style="min-width:90px">状态</th>
            <th style="min-width:140px">供应商</th>
            <th style="min-width:120px">商品</th>
            <th style="min-width:110px">创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in items" :key="e.id" @click="$router.push(`/extractions/${e.id}/confirm`)" style="cursor:pointer">
            <td class="cell-mono whitespace-nowrap">{{ e.applicationNo || '—' }}</td>
            <td class="truncate" style="max-width:220px">{{ e.applicationTitle || '—' }}</td>
            <td><ExtractionStatusBadge :status="e.applicationStatus" /></td>
            <td class="truncate" style="max-width:180px">{{ e.supplierName || '—' }}</td>
            <td class="truncate" style="max-width:160px">{{ e.itemName || '—' }}</td>
            <td class="cell-mono whitespace-nowrap">{{ fmt(e.createTime) }}</td>
          </tr>
          <tr v-if="!items.length">
            <td colspan="6" class="text-center py-8" style="color:var(--muted-foreground)">暂无申请单</td>
          </tr>
        </tbody>
      </table>
      <Pagination :total="total" :page="page" :size="20" @update:page="page=$event; load()" />
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ListTodo, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import FilterChips from '@/components/FilterChips.vue'
import Pagination from '@/components/Pagination.vue'
import ExtractionStatusBadge from '@/components/ExtractionStatusBadge.vue'
import { extractionApi, type ExtractionListItem } from '@/api/extraction'
import dayjs from 'dayjs'

// 与 TaskListView 完全相同的 navs 结构
const navs = [
  { key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '申请单列表', icon: ClipboardList, to: '/extractions' }
]
const filters = ['全部', '待确认', '已确认', '待提交']
const activeFilter = ref('全部')
const items = ref<ExtractionListItem[]>([])
const total = ref(0)
const page = ref(1)

// 与 TaskListView 完全相同的时间格式化
function fmt(s: string | null) { return s ? dayjs(s).format('YYYY-MM-DD') : '—' }

async function load() {
  const res: any = await extractionApi.list({
    page: page.value,
    size: 20,
    status: activeFilter.value === '全部' ? undefined : activeFilter.value
  })
  items.value = res.data.items
  total.value = res.data.total
}

watch(activeFilter, () => { page.value = 1; load() })
onMounted(load)
</script>
