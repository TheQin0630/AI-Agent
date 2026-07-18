<template>
  <div class="app-shell">
    <AppSidebar :navs="navs" />
    <div class="app-main">
      <AppTopbar
        :search-placeholder="searchPlaceholder"
        v-model:search="search"
        @submit-search="onSubmitSearch"
      />
      <section class="app-content">
        <slot />
      </section>
    </div>
  </div>
</template>
<script setup lang="ts">
import type { Component } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppSidebar from "./AppSidebar.vue";
import AppTopbar from "./AppTopbar.vue";

defineProps<{
  navs: Array<{ key: string; label: string; icon: Component; to?: string }>;
  searchPlaceholder?: string;
}>();
const search = defineModel<string>("search", { default: "" });
const route = useRoute();
const router = useRouter();
function onSubmitSearch(value: string) {
  const isExtractionFlow =
    route.path.startsWith("/extractions") ||
    route.path.startsWith("/supplements");
  const isPurchaseOrderFlow = route.path.startsWith("/purchase-orders");
  router.push({
    path: isPurchaseOrderFlow
      ? "/purchase-orders"
      : isExtractionFlow
        ? "/extractions"
        : "/tasks",
    query: value.trim() ? { q: value.trim() } : {},
  });
}
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .app-shell / .app-main / .app-content 样式 */
.app-shell {
  display: block;
  min-height: 100vh;
  background: var(--background);
}
.app-main {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  margin-left: 240px;
  min-width: 0;
}
.app-content {
  flex: 1 0 auto;
  padding: 26px 28px 36px;
  display: flex;
  flex-direction: column;
  gap: calc(var(--spacing) * 5);
  min-height: 0;
  overflow: visible;
  animation: content-in 0.42s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}
@keyframes content-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}
@media (max-width: 1100px) {
  .app-main {
    margin-left: 84px;
  }
}
@media (max-width: 760px) {
  .app-main {
    margin-left: 68px;
  }
  .app-content {
    padding: 20px 14px 30px;
  }
}
</style>
