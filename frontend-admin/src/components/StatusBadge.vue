<template>
  <span class="status-badge" :class="cls">{{ label }}</span>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ status: string }>()
const map: Record<string, { cls: string; label: string }> = {
  PENDING: { cls: 'status-pending', label: '待处理' },
  RUNNING: { cls: 'status-running', label: '进行中' },
  DONE: { cls: 'status-done', label: '已完成' },
  FAILED: { cls: 'status-failed', label: '失败' },
  CONFIRMED: { cls: 'status-done', label: '已确认' }
}
const cur = computed(() => map[props.status] || { cls: 'status-pending', label: props.status })
const cls = computed(() => cur.value.cls)
const label = computed(() => cur.value.label)
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .status-badge 样式 */
.status-badge {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.34rem 0.7rem; border-radius: 999px; font-size: 0.8rem; font-weight: 500;
  white-space: nowrap;
}
.status-done { background: color-mix(in srgb, var(--chart-5) 16%, var(--background)); color: var(--chart-5); }
.status-running { background: color-mix(in srgb, var(--chart-1) 16%, var(--background)); color: var(--chart-1); }
.status-pending { background: var(--muted); color: var(--muted-foreground); }
.status-failed { background: color-mix(in srgb, var(--destructive) 16%, var(--background)); color: var(--destructive); }
</style>
