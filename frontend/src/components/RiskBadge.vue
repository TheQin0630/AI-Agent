<template>
  <span v-if="cls" class="risk-badge" :class="cls">{{ label }}</span>
  <span v-else class="risk-badge risk-none">&mdash;</span>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ level?: string | null }>()
const map: Record<string, { cls: string; label: string }> = {
  HIGH: { cls: 'risk-high', label: '高风险' },
  MEDIUM: { cls: 'risk-medium', label: '中风险' },
  LOW: { cls: 'risk-low', label: '低风险' }
}
const cur = computed(() => (props.level ? map[props.level] : null))
const cls = computed(() => cur.value?.cls || '')
const label = computed(() => cur.value?.label || '')
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .risk-badge 样式 */
.risk-badge {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.34rem 0.7rem; border-radius: 999px; font-size: 0.8rem; font-weight: 500;
  white-space: nowrap;
}
.risk-high { background: color-mix(in srgb, var(--chart-2) 16%, var(--background)); color: var(--chart-2); }
.risk-medium { background: color-mix(in srgb, var(--chart-3) 16%, var(--background)); color: var(--chart-3); }
.risk-low { background: color-mix(in srgb, var(--chart-5) 16%, var(--background)); color: var(--chart-5); }
.risk-none { color: var(--muted-foreground); }
</style>
