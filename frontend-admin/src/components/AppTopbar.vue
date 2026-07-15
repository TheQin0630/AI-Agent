<template>
  <header class="app-topbar">
    <div class="topbar-left">
      <div class="topbar-search">
        <Search :size="16" :style="{ color: 'var(--muted-foreground)', flexShrink: 0 }" />
        <input type="text" v-model="search" :placeholder="searchPlaceholder" aria-label="搜索" />
      </div>
    </div>
    <div class="topbar-right">
      <button class="topbar-icon-btn" type="button" aria-label="通知">
        <Bell :size="18" />
      </button>
      <router-link to="/admin" class="topbar-avatar" aria-label="用户头像">{{ avatarLetter }}</router-link>
    </div>
  </header>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { Search, Bell } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

defineProps<{ searchPlaceholder?: string }>()
const search = defineModel<string>('search', { default: '' })

const auth = useAuthStore()
const avatarLetter = computed(() => auth.user?.username?.charAt(0)?.toUpperCase() || 'A')
</script>
<style scoped>
/* 拷贝自 design/pages/管理概览.html 的 .app-topbar 样式 */
.app-topbar {
  height: 76px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  gap: calc(var(--spacing) * 4); padding: 0 calc(var(--spacing) * 6);
  background: var(--background);
}
.topbar-left, .topbar-right { display: flex; align-items: center; gap: calc(var(--spacing) * 3); }
.topbar-search {
  display: flex; align-items: center; gap: calc(var(--spacing) * 3);
  width: min(420px, 46vw); padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
  border: 1px solid var(--input); border-radius: 999px; background: var(--popover);
}
.topbar-search input { border: none; outline: none; background: transparent; color: var(--foreground); width: 100%; font: inherit; }
.topbar-icon-btn {
  width: 40px; height: 40px; display: grid; place-items: center;
  border: 1px solid var(--border); background: var(--card); color: var(--foreground);
  border-radius: var(--radius); cursor: pointer; transition: background 0.15s ease;
}
.topbar-icon-btn:hover { background: var(--accent); }
.topbar-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  display: grid; place-items: center;
  background: var(--primary); color: var(--primary-foreground);
  font-weight: 700; font-size: 0.9rem;
  cursor: pointer; text-decoration: none;
}
@media (max-width: 1100px) { .topbar-search { width: min(280px, 100%); } }
@media (max-width: 760px) {
  .app-topbar { padding-inline: calc(var(--spacing) * 4); }
  .topbar-search { display: none; }
}
</style>
