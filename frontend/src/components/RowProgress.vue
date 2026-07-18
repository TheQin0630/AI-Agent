<template><div class="row-progress" :aria-label="'进度 '+value+'%'"><span>{{ value }}%</span><div><i :style="{width:value+'%'}" /></div></div></template>
<script setup lang="ts">
defineProps<{ value: number }>()
</script>
<style scoped>.row-progress{display:flex;align-items:center;gap:8px;min-width:104px}.row-progress>span{width:30px;font:500 11px var(--font-mono);color:var(--muted-foreground)}.row-progress>div{height:6px;flex:1;overflow:hidden;border-radius:999px;background:#e9eef5}.row-progress i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#4f9cff,var(--primary));animation:grow .75s cubic-bezier(.2,.8,.2,1) both}@keyframes grow{from{max-width:0}}</style>
