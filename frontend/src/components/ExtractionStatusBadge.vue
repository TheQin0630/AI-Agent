<template>
  <span class="extraction-status-badge" :class="cls">{{ label }}</span>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ status: string }>()

// 申请单状态映射（中文，仅用于申请单相关页面，不影响任务页面的 StatusBadge）
// 样式与 StatusBadge 保持一致：color-mix(in srgb, <chart-color> 16%, var(--background))
const map: Record<string, { cls: string; label: string }> = {
  '待确认': { cls: 'status-running', label: '待确认' },
  '已确认': { cls: 'status-done', label: '已确认' },
  '待提交': { cls: 'status-pending', label: '待提交' },
  '创建失败': { cls: 'status-failed', label: '创建失败' }
}
const cur = computed(() => map[props.status] || { cls: 'status-pending', label: props.status })
const cls = computed(() => cur.value.cls)
const label = computed(() => cur.value.label)
</script>
<style scoped>
/* 与 StatusBadge.vue 的样式完全一致 */
.extraction-status-badge {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.34rem 0.7rem; border-radius: 999px; font-size: 0.8rem; font-weight: 500;
  white-space: nowrap;
}
.status-done { background: color-mix(in srgb, var(--chart-5) 16%, var(--background)); color: var(--chart-5); }
.status-running { background: color-mix(in srgb, var(--chart-1) 16%, var(--background)); color: var(--chart-1); }
.status-pending { background: var(--muted); color: var(--muted-foreground); }
.status-failed { background: color-mix(in srgb, var(--destructive) 16%, var(--background)); color: var(--destructive); }
</style>
