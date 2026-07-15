<template>
  <div class="fixed top-5 right-5 z-50 flex flex-col gap-2">
    <div v-for="t in toast.items" :key="t.id"
      class="px-4 py-3 text-sm font-medium shadow-sm border"
      :style="boxStyle(t.type)"
      @click="toast.dismiss(t.id)">
      {{ t.message }}
    </div>
  </div>
</template>
<script setup lang="ts">
import { useToastStore, type ToastType } from '@/stores/toast'
const toast = useToastStore()
function boxStyle(t: ToastType) {
  const map: Record<ToastType, string> = {
    success: `background:var(--card);color:var(--chart-5);border-color:var(--border)`,
    error:   `background:var(--card);color:var(--destructive);border-color:var(--border)`,
    warning: `background:var(--card);color:var(--chart-3);border-color:var(--border)`,
    info:    `background:var(--card);color:var(--foreground);border-color:var(--border)`
  }
  return `border-radius:var(--radius);${map[t]}`
}
</script>
