<template>
  <div class="flex flex-col gap-1.5">
    <label class="text-xs font-medium" style="color:var(--muted-foreground)">{{ label }}</label>
    <div class="flex items-center gap-2 rounded-md"
      :style="`border:1px solid var(--input);background:var(--popover);padding:0 calc(var(--spacing)*3);height:32px`">
      <component :is="icon" :size="16" :style="{ color: 'var(--muted-foreground)', flexShrink: 0 }" />
      <input :type="type" v-model="model" :placeholder="placeholder"
        class="bg-transparent border-0 outline-none text-sm w-full" style="color:var(--foreground);font-family:var(--font-sans)" />
    </div>
  </div>
</template>
<script setup lang="ts">
import type { Component } from 'vue'
defineProps<{ label: string; icon: Component; type?: string; placeholder?: string }>()
const model = defineModel<string>()
</script>
