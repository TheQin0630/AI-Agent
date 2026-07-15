<template>
  <aside class="app-sidebar">
    <div class="sidebar-brand">
      <div class="sidebar-brand-mark">CC</div>
      <div class="sidebar-brand-copy">
        <h1>合同对比 Agent</h1>
        <p>Contract Comparison</p>
      </div>
    </div>
    <nav class="sidebar-nav" aria-label="Main navigation">
      <router-link
        v-for="n in navs"
        :key="n.key"
        :to="n.to || '#'"
        class="sidebar-nav-btn"
        active-class="active"
      >
        <span class="nav-glyph">
          <component :is="n.icon" :size="20" />
        </span>
        <span class="nav-label">{{ n.label }}</span>
      </router-link>
    </nav>
    <div class="sidebar-foot">智能合同对比，精准风险分析</div>
  </aside>
</template>
<script setup lang="ts">
import type { Component } from 'vue'
defineProps<{ navs: Array<{ key: string; label: string; icon: Component; to?: string }> }>()
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .app-sidebar 样式 */
.app-sidebar {
  background: var(--sidebar);
  color: var(--sidebar-foreground);
  border-right: 1px solid var(--sidebar-border);
  padding: calc(var(--spacing) * 6);
  display: flex;
  flex-direction: column;
  gap: calc(var(--spacing) * 5);
  transition: width 0.24s ease, padding 0.24s ease;
  overflow: hidden;
}
.sidebar-brand { display: flex; align-items: center; gap: calc(var(--spacing) * 3); }
.sidebar-brand-mark {
  width: 40px; height: 40px;
  border-radius: calc(var(--radius) + 8px);
  display: grid; place-items: center;
  background: var(--sidebar-primary);
  color: var(--sidebar-primary-foreground);
  font-weight: 700; font-size: 0.95rem;
  flex-shrink: 0;
}
.sidebar-brand-copy h1 { margin: 0; font-size: 1rem; }
.sidebar-brand-copy p { margin: 0.18rem 0 0; color: var(--muted-foreground); font-size: 0.82rem; }
.sidebar-nav { display: flex; flex-direction: column; gap: calc(var(--spacing) * 2); }
.sidebar-nav-btn {
  display: flex; align-items: center; gap: calc(var(--spacing) * 3);
  width: 100%; padding: calc(var(--spacing) * 3.5) calc(var(--spacing) * 4);
  border: none; background: transparent; color: inherit;
  border-radius: var(--radius); text-align: left;
  cursor: pointer; font: inherit; transition: background 0.2s ease, color 0.2s ease;
  text-decoration: none;
}
.sidebar-nav-btn:hover { background: var(--sidebar-accent); }
.sidebar-nav-btn.active { background: var(--sidebar-primary); color: var(--sidebar-primary-foreground); }
.sidebar-nav-btn .nav-glyph { width: 1.2rem; text-align: center; flex: 0 0 1.2rem; display: grid; place-items: center; }
.sidebar-nav-btn .nav-label { white-space: nowrap; }
.sidebar-foot {
  margin-top: auto; padding-top: calc(var(--spacing) * 4);
  border-top: 1px solid var(--sidebar-border);
  font-size: 0.82rem; color: var(--muted-foreground);
}
@media (max-width: 1100px) {
  .app-sidebar { padding-inline: calc(var(--spacing) * 3.5); }
  .sidebar-brand-copy h1,
  .sidebar-brand-copy p,
  .sidebar-nav-btn .nav-label,
  .sidebar-foot { opacity: 0; max-width: 0; overflow: hidden; }
}
</style>
