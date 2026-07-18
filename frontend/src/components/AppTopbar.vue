<template>
  <header ref="topbarRef" class="app-topbar">
    <label class="topbar-search"
      ><Search :size="17" /><input
        ref="searchInput"
        v-model="search"
        type="search"
        :placeholder="searchPlaceholder"
        aria-label="搜索"
        @keydown.enter="$emit('submit-search', search)"
      /><button
        v-if="search"
        class="search-clear"
        type="button"
        aria-label="清空搜索"
        @click="search = ''"
      >
        <X :size="14" /></button
      ></label
    >
    <div class="topbar-right">
      <span class="live-status"><i />服务正常</span
      ><button
        class="topbar-icon-btn"
        type="button"
        aria-label="通知"
        :aria-expanded="notificationOpen"
        @click="notificationOpen = !notificationOpen"
      >
        <Bell :size="18" /><b v-if="notificationCount">{{
          notificationLabel
        }}</b></button
      ><Transition name="popover">
        <section
          v-if="notificationOpen"
          class="notification-panel"
          aria-label="业务待办"
        >
          <div class="notification-head">
            <div>
              <strong>业务待办</strong><small>根据当前任务实时汇总</small>
            </div>
            <button
              type="button"
              aria-label="刷新待办"
              @click="refreshNotifications"
            >
              <RefreshCw :size="14" :class="{ spin: refreshing }" />
            </button>
          </div>
          <router-link
            :to="{ path: '/tasks', query: { status: '进行中' } }"
            @click="notificationOpen = false"
          >
            <span class="notice-icon blue"><ScanSearch :size="17" /></span>
            <span
              ><strong>合同处理任务</strong
              ><small>{{ pendingTaskCount }} 项等待处理或分析</small></span
            >
            <ChevronRight :size="15" />
          </router-link>
          <router-link
            :to="{ path: '/extractions', query: { status: '待确认' } }"
            @click="notificationOpen = false"
          >
            <span class="notice-icon amber"><ClipboardCheck :size="17" /></span>
            <span
              ><strong>申请单人工确认</strong
              ><small>{{ pendingExtractionCount }} 单等待审核</small></span
            >
            <ChevronRight :size="15" />
          </router-link>
          <p v-if="!notificationCount" class="notice-empty">
            当前没有待处理业务
          </p>
        </section> </Transition
      ><router-link to="/me" class="profile"
        ><span class="topbar-avatar">{{ avatarLetter }}</span
        ><span class="profile-copy"
          ><strong>{{ auth.user?.username || "用户" }}</strong
          ><small>采购专员</small></span
        ><ChevronDown :size="14"
      /></router-link>
    </div>
  </header>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import {
  Bell,
  ChevronDown,
  ChevronRight,
  ClipboardCheck,
  RefreshCw,
  ScanSearch,
  Search,
  X,
} from "lucide-vue-next";
import { useAuthStore } from "@/stores/auth";
import { authApi } from "@/api/auth";
import { taskApi } from "@/api/task";
import { extractionApi } from "@/api/extraction";
defineProps<{ searchPlaceholder?: string }>();
defineEmits<{ "submit-search": [value: string] }>();
const search = defineModel<string>("search", { default: "" });
const auth = useAuthStore();
const searchInput = ref<HTMLInputElement | null>(null);
const topbarRef = ref<HTMLElement | null>(null);
const notificationOpen = ref(false);
const pendingTaskCount = ref(0);
const pendingExtractionCount = ref(0);
const refreshing = ref(false);
const avatarLetter = computed(
  () => auth.user?.username?.charAt(0)?.toUpperCase() || "U",
);
const notificationCount = computed(
  () => pendingTaskCount.value + pendingExtractionCount.value,
);
const notificationLabel = computed(() =>
  notificationCount.value > 99 ? "99+" : String(notificationCount.value),
);
async function refreshNotifications() {
  refreshing.value = true;
  try {
    const [taskRes, extractionRes]: any[] = await Promise.all([
      taskApi.list({ page: 1, size: 100 }),
      extractionApi.list({ page: 1, size: 100, status: "待确认" }),
    ]);
    pendingTaskCount.value = (taskRes.data.items || []).filter((task: any) =>
      ["PENDING", "RUNNING", "FAILED"].includes(task.status),
    ).length;
    pendingExtractionCount.value = Number(extractionRes.data.total || 0);
  } catch {
    pendingTaskCount.value = 0;
    pendingExtractionCount.value = 0;
  } finally {
    refreshing.value = false;
  }
}
async function hydrateCurrentUser() {
  if (!auth.token || auth.user) return;
  try {
    const response: any = await authApi.me();
    auth.setUser(response.data);
  } catch {
    // The global HTTP interceptor owns expired-session handling.
  }
}
function onKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    searchInput.value?.focus();
  }
  if (event.key === "Escape") notificationOpen.value = false;
}
function onPointerDown(event: PointerEvent) {
  if (!topbarRef.value?.contains(event.target as Node))
    notificationOpen.value = false;
}
onMounted(() => {
  hydrateCurrentUser();
  refreshNotifications();
  document.addEventListener("keydown", onKeydown);
  document.addEventListener("pointerdown", onPointerDown);
});
onBeforeUnmount(() => {
  document.removeEventListener("keydown", onKeydown);
  document.removeEventListener("pointerdown", onPointerDown);
});
</script>
<style scoped>
.app-topbar {
  height: 72px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  position: sticky;
  top: 0;
  z-index: 20;
}
.topbar-search {
  display: flex;
  align-items: center;
  gap: 9px;
  width: min(450px, 44vw);
  height: 40px;
  padding: 0 16px;
  border: 1px solid #e2e4e8;
  border-radius: 999px;
  background: #f7f7f8;
  color: var(--muted-foreground);
  transition:
    transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1),
    border-color 0.22s ease,
    background 0.22s ease,
    box-shadow 0.22s ease;
}
.topbar-search:hover {
  transform: translateY(-1px);
  border-color: #cdd8e8;
  background: #fafbfc;
  box-shadow: 0 6px 18px rgba(25, 55, 94, 0.07);
}
.topbar-search:focus-within {
  transform: translateY(-1px);
  background: white;
  border-color: #a9c9f5;
  box-shadow: 0 0 0 4px rgba(23, 105, 224, 0.08);
}
.topbar-search input {
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--foreground);
  width: 100%;
  font: inherit;
  font-size: 13px;
}
.search-clear {
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--muted-foreground);
  cursor: pointer;
}
.search-clear:hover {
  background: var(--muted);
  color: var(--foreground);
}
.topbar-right,
.profile {
  display: flex;
  align-items: center;
  gap: 11px;
}
.topbar-right {
  position: relative;
}
.live-status {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--muted-foreground);
  font-size: 12px;
}
.live-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 4px #dcfce7;
}
.topbar-icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border);
  background: white;
  color: var(--foreground);
  border-radius: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.topbar-icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}
.topbar-icon-btn b {
  position: absolute;
  right: -3px;
  top: -4px;
  min-width: 16px;
  height: 16px;
  border-radius: 999px;
  background: #ef4444;
  color: white;
  font-size: 9px;
  display: grid;
  place-items: center;
  border: 2px solid white;
}
.notification-panel {
  position: absolute;
  z-index: 30;
  top: 52px;
  right: 118px;
  width: min(340px, calc(100vw - 32px));
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 22px 55px rgba(24, 52, 84, 0.16);
  backdrop-filter: blur(18px);
}
.notification-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 8px 10px;
}
.notification-head > div,
.notification-panel a > span:nth-child(2) {
  display: flex;
  flex-direction: column;
}
.notification-head strong,
.notification-panel a strong {
  font-size: 12px;
}
.notification-head small,
.notification-panel a small {
  margin-top: 2px;
  color: var(--muted-foreground);
  font-size: 10px;
}
.notification-head button {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 10px;
  background: var(--muted);
  color: var(--muted-foreground);
  cursor: pointer;
}
.notification-panel a {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 13px;
  color: inherit;
  text-decoration: none;
  transition:
    background 0.2s ease,
    transform 0.2s ease;
}
.notification-panel a:hover {
  background: #f5f8fc;
  transform: translateX(2px);
}
.notice-icon {
  width: 36px;
  height: 36px;
  display: grid !important;
  place-items: center;
  border-radius: 12px;
}
.notice-icon.blue {
  background: #e8f1ff;
  color: var(--primary);
}
.notice-icon.amber {
  background: #fff5df;
  color: #d97706;
}
.notice-empty {
  margin: 4px 8px 2px;
  padding-top: 9px;
  border-top: 1px solid var(--border);
  color: var(--muted-foreground);
  font-size: 11px;
  text-align: center;
}
.popover-enter-active,
.popover-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.popover-enter-from,
.popover-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}
.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.profile {
  padding: 5px 8px 5px 5px;
  border-radius: 15px;
  color: inherit;
  text-decoration: none;
  transition: background 0.2s;
}
.profile:hover {
  background: var(--muted);
}
.topbar-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #398bf1, #1769e0);
  color: white;
  font-weight: 700;
}
.profile-copy {
  display: flex;
  flex-direction: column;
  min-width: 72px;
}
.profile-copy strong {
  font-size: 12px;
}
.profile-copy small {
  font-size: 10px;
  color: var(--muted-foreground);
}
@media (max-width: 760px) {
  .app-topbar {
    padding: 0 16px;
  }
  .topbar-search {
    width: 42px;
    height: 40px;
    justify-content: center;
    padding: 0;
  }
  .topbar-search input,
  .live-status,
  .profile-copy,
  .profile > svg {
    display: none;
  }
  .notification-panel {
    position: fixed;
    top: 66px;
    right: 12px;
  }
}
</style>
