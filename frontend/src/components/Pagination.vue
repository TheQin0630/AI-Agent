<template>
  <div class="pagination">
    <span>共 {{ total }} 条</span>
    <div class="pagination-pages">
      <button class="page-btn" type="button" aria-label="上一页" :disabled="page <= 1" @click="go(page - 1)">
        <ChevronLeft :size="14" />
      </button>
      <button
        v-for="p in pages"
        :key="p"
        class="page-btn"
        :class="{ active: p === page }"
        type="button"
        @click="go(p)"
      >{{ p }}</button>
      <button class="page-btn" type="button" aria-label="下一页" :disabled="page >= totalPages" @click="go(page + 1)">
        <ChevronRight :size="14" />
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps<{ total: number; page: number; size: number }>()
const emit = defineEmits<{ (e: 'update:page', v: number): void }>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
const pages = computed(() => {
  const arr: number[] = []
  const tp = totalPages.value
  let start = Math.max(1, props.page - 2)
  let end = Math.min(tp, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) arr.push(i)
  return arr
})
function go(p: number) {
  if (p < 1 || p > totalPages.value || p === props.page) return
  emit('update:page', p)
}
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .pagination 样式 */
.pagination {
  display: flex; align-items: center; justify-content: space-between;
  padding: calc(var(--spacing) * 3) calc(var(--spacing) * 5);
  font-size: 0.85rem; color: var(--muted-foreground);
}
.pagination-pages { display: flex; gap: 6px; align-items: center; }
.page-btn {
  min-width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--card); color: var(--foreground); cursor: pointer; font: inherit;
  transition: background 0.15s ease;
}
.page-btn:hover:not(:disabled) { background: var(--accent); color: var(--accent-foreground); }
.page-btn.active { background: var(--primary); color: var(--primary-foreground); border-color: var(--primary); }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
