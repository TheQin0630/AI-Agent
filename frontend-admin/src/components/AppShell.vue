<template>
  <div class="app-shell">
    <AppSidebar :navs="navs" />
    <div class="app-main">
      <AppTopbar :search-placeholder="searchPlaceholder" v-model:search="search" />
      <section class="app-content">
        <slot />
      </section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { Component } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'

defineProps<{ navs: Array<{ key: string; label: string; icon: Component; to?: string }>; searchPlaceholder?: string }>()
const search = ref('')
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .app-shell / .app-main / .app-content 样式 */
.app-shell { display: grid; grid-template-columns: 260px 1fr; height: 100vh; }
.app-main { display: flex; flex-direction: column; min-width: 0; }
.app-content {
  padding: calc(var(--spacing) * 6);
  display: flex; flex-direction: column; gap: calc(var(--spacing) * 5);
  min-height: 0; overflow-y: auto;
}
@media (max-width: 1100px) { .app-shell { grid-template-columns: 84px 1fr; } }
</style>
