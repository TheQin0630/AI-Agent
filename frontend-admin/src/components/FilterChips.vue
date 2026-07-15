<template>
  <div class="filter-row no-scrollbar">
    <button
      v-for="opt in options"
      :key="opt"
      type="button"
      class="filter-chip"
      :class="{ active: modelValue === opt }"
      @click="$emit('update:modelValue', opt)"
    >{{ opt }}</button>
  </div>
</template>
<script setup lang="ts">
defineProps<{ options: string[]; modelValue: string }>()
defineEmits<{ (e: 'update:modelValue', v: string): void }>()
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .filter-row / .filter-chip 样式 */
.filter-row {
  display: flex; align-items: center; gap: calc(var(--spacing) * 2);
  padding: calc(var(--spacing) * 4) calc(var(--spacing) * 5);
  border-bottom: 1px solid var(--border);
  flex-wrap: nowrap; overflow-x: auto;
}
.filter-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: calc(var(--spacing) * 2.5) calc(var(--spacing) * 4);
  border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--card); color: var(--muted-foreground);
  cursor: pointer; font: 500 0.84rem/1 var(--font-sans);
  white-space: nowrap; transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}
.filter-chip:hover { background: var(--accent); color: var(--accent-foreground); }
.filter-chip.active { background: var(--primary); border-color: var(--primary); color: var(--primary-foreground); }
</style>
