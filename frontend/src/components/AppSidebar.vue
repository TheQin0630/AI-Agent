<template>
  <aside class="app-sidebar">
    <div class="sidebar-brand">
      <div class="sidebar-brand-mark"><Layers3 :size="21" /></div>
      <div class="sidebar-brand-copy">
        <h1>审批中心</h1>
        <p>Approval Center</p>
      </div>
    </div>
    <nav class="sidebar-nav" aria-label="主导航">
      <router-link
        v-for="n in displayNavs"
        :key="n.key"
        :to="n.to"
        class="sidebar-nav-btn"
        :class="{ active: isActive(n.to) }"
      >
        <span class="nav-glyph"><component :is="n.icon" :size="20" /></span
        ><span class="nav-label">{{ n.label }}</span
        ><ChevronRight class="nav-arrow" :size="15" />
      </router-link>
    </nav>
    <div class="sidebar-foot">
      <ShieldCheck :size="18" />
      <div><strong>智能风控在线</strong><span>数据安全 · 全程可追溯</span></div>
    </div>
  </aside>
</template>
<script setup lang="ts">
import { computed } from "vue";
import type { Component } from "vue";
import { useRoute } from "vue-router";
import {
  ChevronRight,
  CirclePlus,
  ClipboardList,
  FileCheck2,
  Inbox,
  Layers3,
  ListTodo,
  ShoppingCart,
  ShieldCheck,
  UserRound,
} from "lucide-vue-next";
const props = defineProps<{
  navs: Array<{ key: string; label: string; icon: Component; to?: string }>;
}>();
const route = useRoute();
const displayNavs = computed(() => {
  const supplied = new Map(props.navs.map((item) => [item.key, item]));
  return [
    supplied.get("tasks") || {
      key: "tasks",
      label: "合同对比",
      icon: ListTodo,
      to: "/tasks",
    },
    {
      key: "task-create",
      label: "新建对比",
      icon: CirclePlus,
      to: "/tasks/new",
    },
    {
      ...(supplied.get("extractions") || {
        key: "extractions",
        icon: ClipboardList,
        to: "/extractions",
      }),
      label: "采购申请",
    },
    supplied.get("purchase-orders") || {
      key: "purchase-orders",
      label: "采购订单",
      icon: ShoppingCart,
      to: "/purchase-orders",
    },
    {
      key: "review-center",
      label: "待办中心",
      icon: Inbox,
      to: "/extractions?status=待确认",
    },
    {
      key: "risk-reports",
      label: "风险报告",
      icon: FileCheck2,
      to: "/tasks?status=已完成",
    },
    { key: "profile", label: "个人中心", icon: UserRound, to: "/me" },
  ].map((item) => ({ ...item, to: item.to || "/tasks" }));
});
function isActive(to: string) {
  if (to.includes("?")) return route.fullPath === to;
  if (to === "/tasks")
    return (
      (route.path === "/tasks" && !route.query.status) ||
      /^\/tasks\/\d+$/.test(route.path)
    );
  if (to === "/extractions")
    return (
      (route.path === "/extractions" && !route.query.status) ||
      route.path.startsWith("/extractions/") ||
      route.path.startsWith("/supplements")
    );
  if (to === "/purchase-orders")
    return route.path.startsWith("/purchase-orders");
  return route.path === to;
}
</script>
<style scoped>
.app-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 40;
  width: 240px;
  height: 100vh;
  border-right: 1px solid #e6ebf2;
  background: #fbfcfe;
  color: #172033;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 28px;
  overflow: hidden;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
}
.sidebar-brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 15px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #2785f5, #0964cf);
  color: white;
  box-shadow: 0 8px 20px rgba(23, 105, 224, 0.18);
  flex-shrink: 0;
}
.sidebar-brand-copy h1 {
  margin: 0;
  font-size: 17px;
  letter-spacing: 0.02em;
}
.sidebar-brand-copy p {
  margin: 3px 0 0;
  color: #5f7088;
  font-size: 12.5px;
  font-weight: 500;
  white-space: nowrap;
}
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sidebar-nav-btn {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  min-height: 48px;
  padding: 13px 14px;
  border: 1px solid transparent;
  color: #53647a;
  border-radius: 14px;
  text-decoration: none;
  transition: all 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.sidebar-nav-btn:hover {
  color: #1769e0;
  background: #edf4ff;
  transform: translateX(2px);
}
.sidebar-nav-btn.active {
  color: #155fc9;
  background: #e8f1ff;
  border-color: #c9ddfb;
  box-shadow: 0 6px 16px rgba(23, 105, 224, 0.1);
}
.nav-glyph {
  width: 22px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}
.nav-label {
  font-size: 14px;
  font-weight: 400;
  white-space: nowrap;
}
.sidebar-nav-btn.active .nav-label {
  font-weight: 600;
}
.nav-arrow {
  margin-left: auto;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s;
}
.active .nav-arrow {
  opacity: 0.7;
  transform: none;
}
.sidebar-foot {
  margin-top: auto;
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px 14px;
  border: 1px solid #d8e8ff;
  border-radius: 16px;
  background: #f0f6ff;
  color: #1769e0;
}
.sidebar-foot div {
  display: flex;
  flex-direction: column;
}
.sidebar-foot strong {
  font-size: 14px;
  color: #183b68;
}
.sidebar-foot span {
  margin-top: 2px;
  font-size: 12px;
  color: #607692;
  white-space: nowrap;
}
@media (max-width: 1100px) {
  .app-sidebar {
    width: 84px;
    padding-inline: 13px;
  }
  .sidebar-brand-copy,
  .nav-label,
  .nav-arrow,
  .sidebar-foot div {
    display: none;
  }
  .sidebar-brand {
    padding: 0;
  }
  .sidebar-nav-btn {
    justify-content: center;
  }
  .sidebar-foot {
    justify-content: center;
  }
}
@media (max-width: 760px) {
  .app-sidebar {
    width: 68px;
  }
}
</style>
