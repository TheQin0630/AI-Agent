<template>
  <AppShell :navs="navs" search-placeholder="搜索任务或申请单…">
    <header class="page-heading">
      <div>
        <span class="eyebrow">ACCOUNT</span>
        <h1>个人中心</h1>
        <p>管理当前登录账号与工作台会话。</p>
      </div>
      <span class="account-state"><i />账号正常</span>
    </header>

    <section class="profile-card">
      <div class="profile-hero">
        <span class="large-avatar">{{ avatarLetter }}</span>
        <div>
          <h2>{{ user?.username || "加载中…" }}</h2>
          <p>{{ roleLabel }} · 采购合同协同工作台</p>
        </div>
      </div>

      <div class="account-grid">
        <article>
          <span>用户 ID</span>
          <strong>{{ user?.id ?? "—" }}</strong>
        </article>
        <article>
          <span>用户名</span>
          <strong>{{ user?.username ?? "—" }}</strong>
        </article>
        <article>
          <span>账号角色</span>
          <strong>{{ roleLabel }}</strong>
        </article>
      </div>

      <footer>
        <div>
          <strong>退出当前账号</strong>
          <small>退出后需要重新输入账号密码才能进入工作台。</small>
        </div>
        <button class="logout-button" type="button" @click="onLogout">
          <LogOut :size="16" />退出登录
        </button>
      </footer>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ClipboardList, ListTodo, LogOut } from "lucide-vue-next";
import AppShell from "@/components/AppShell.vue";
import { authApi } from "@/api/auth";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";

const navs = [
  { key: "tasks", label: "合同对比", icon: ListTodo, to: "/tasks" },
  {
    key: "extractions",
    label: "采购申请单",
    icon: ClipboardList,
    to: "/extractions",
  },
];
const auth = useAuthStore();
const toast = useToastStore();
const router = useRouter();
const user = ref(auth.user);

const roleLabel = computed(() =>
  user.value?.role === "ADMIN" ? "管理员" : "采购专员",
);
const avatarLetter = computed(
  () => user.value?.username?.charAt(0)?.toUpperCase() || "U",
);

async function loadMe() {
  try {
    const response: any = await authApi.me();
    user.value = response.data;
    auth.setUser(response.data);
  } catch {
    // The global HTTP interceptor owns authentication error feedback.
  }
}
function onLogout() {
  auth.logout();
  toast.success("已退出登录");
  router.push("/login");
}
onMounted(loadMe);
</script>

<style scoped>
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}
.eyebrow {
  color: var(--primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.page-heading h1 {
  margin: 6px 0 4px;
  font-size: 30px;
  letter-spacing: -0.04em;
}
.page-heading p {
  margin: 0;
  color: var(--muted-foreground);
}
.account-state {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #ccefe0;
  border-radius: 999px;
  background: #effcf6;
  color: #087a55;
  font-size: 12px;
  font-weight: 700;
}
.account-state i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.12);
}
.profile-card {
  max-width: 860px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 24px;
  background: var(--card);
  box-shadow: var(--shadow);
}
.profile-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(
      circle at 88% 12%,
      rgba(65, 137, 241, 0.15),
      transparent 30%
    ),
    linear-gradient(140deg, #f8fbff, #fff);
}
.large-avatar {
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  border-radius: 21px;
  background: linear-gradient(145deg, #398bf1, #1769e0);
  box-shadow: 0 12px 28px rgba(23, 105, 224, 0.25);
  color: white;
  font-size: 24px;
  font-weight: 800;
}
.profile-hero h2 {
  margin: 0 0 5px;
  font-size: 21px;
}
.profile-hero p {
  margin: 0;
  color: var(--muted-foreground);
  font-size: 13px;
}
.account-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 22px 28px 28px;
}
.account-grid article {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 16px;
  border: 1px solid #e9eef5;
  border-radius: 16px;
  background: #f8fafc;
}
.account-grid span,
.profile-card footer small {
  color: var(--muted-foreground);
  font-size: 11px;
}
.account-grid strong {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}
.profile-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 28px;
  border-top: 1px solid var(--border);
  background: #fbfcfe;
}
.profile-card footer > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.profile-card footer strong {
  font-size: 13px;
}
.logout-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 13px;
  border: 1px solid #f1c7c7;
  border-radius: 12px;
  background: #fff;
  color: #c83c3c;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.logout-button:hover {
  transform: translateY(-1px);
  background: #fff6f6;
  box-shadow: 0 8px 20px rgba(200, 60, 60, 0.1);
}
@media (max-width: 680px) {
  .page-heading {
    align-items: flex-start;
  }
  .account-state {
    display: none;
  }
  .account-grid {
    grid-template-columns: 1fr;
  }
  .profile-card footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
