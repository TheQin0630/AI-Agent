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
  padding: 0;
  flex-wrap: nowrap; overflow-x: auto;
}
.filter-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: calc(var(--spacing) * 2.5) calc(var(--spacing) * 4);
  border: 1px solid var(--border); border-radius: 999px;
  background: var(--card); color: var(--muted-foreground);
  cursor: pointer; font: 500 0.84rem/1 var(--font-sans);
  white-space: nowrap; transition: background .22s ease,border-color .22s ease,color .22s ease,transform .22s ease,box-shadow .22s ease;
}
.filter-chip:hover { background: var(--accent); color: var(--accent-foreground); transform:translateY(-1px) }
.filter-chip.active {
  padding-inline: calc(var(--spacing) * 4.5);
  background: #e8f1ff;
  border-color: #c9ddfb;
  color: #155fc9;
  box-shadow: 0 5px 12px rgba(23,105,224,.1);
  font-size: 0.9rem;
  font-weight: 700;
}
</style>
