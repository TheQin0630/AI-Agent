<template>
  <div class="flex flex-col gap-1.5">
    <label class="text-xs font-medium" style="color:var(--muted-foreground)">{{ label }}</label>
    <div
      class="file-drop"
      :class="{ active: isDragover, filled: !!model }"
      @click="pick"
      @dragover.prevent="isDragover = true"
      @dragleave.prevent="isDragover = false"
      @drop.prevent="onDrop"
    >
      <Upload :size="18" :style="{ color: 'var(--muted-foreground)', flexShrink: 0 }" />
      <span v-if="model" class="text-sm truncate" style="color:var(--foreground)">{{ model.name }}</span>
      <span v-else class="text-sm" style="color:var(--muted-foreground)">{{ placeholder || '点击或拖拽文件到此处上传' }}</span>
      <span v-if="model" class="text-xs ml-auto whitespace-nowrap" style="color:var(--primary)">替换</span>
    </div>
    <input ref="inputRef" type="file" class="hidden" @change="onPick" />
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { Upload } from 'lucide-vue-next'

defineProps<{ label: string; placeholder?: string }>()
const model = defineModel<File | null>({ default: null })
const isDragover = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)

function pick() {
  inputRef.value?.click()
}
function onPick(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) model.value = f
  ;(e.target as HTMLInputElement).value = ''
}
function onDrop(e: DragEvent) {
  isDragover.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) model.value = f
}
</script>
<style scoped>
.file-drop {
  display: flex; align-items: center; gap: calc(var(--spacing) * 3);
  padding: calc(var(--spacing) * 4) calc(var(--spacing) * 4);
  border: 1.5px dashed var(--input); border-radius: var(--radius);
  background: var(--popover); cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.file-drop:hover, .file-drop.active { background: var(--accent); border-color: var(--primary); }
.file-drop.filled { border-style: solid; }
</style>
