<template>
  <AppShell :navs="navs" search-placeholder="搜索任务...">
    <div class="hero">
      <div>
        <h2>个人中心</h2>
        <p>查看账号信息与退出登录</p>
      </div>
    </div>
    <article class="content-card" style="max-width:600px;padding:calc(var(--spacing)*6)">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div class="flex flex-col">
          <span class="text-xs mb-1" style="color:var(--muted-foreground)">用户 ID</span>
          <span class="font-medium" style="color:var(--foreground)">{{ user?.id ?? '—' }}</span>
        </div>
        <div class="flex flex-col">
          <span class="text-xs mb-1" style="color:var(--muted-foreground)">用户名</span>
          <span class="font-medium" style="color:var(--foreground)">{{ user?.username ?? '—' }}</span>
        </div>
        <div class="flex flex-col">
          <span class="text-xs mb-1" style="color:var(--muted-foreground)">角色</span>
          <span class="font-medium" style="color:var(--foreground)">{{ roleLabel }}</span>
        </div>
      </div>
      <button class="action-btn ghost" @click="onLogout">退出登录</button>
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ListTodo } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const navs = [{ key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' }]
const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()
const user = ref(auth.user)

const roleLabel = computed(() => (user.value?.role === 'ADMIN' ? '管理员' : '普通用户'))

async function loadMe() {
  try {
    const res: any = await authApi.me()
    user.value = res.data
    auth.setUser(res.data)
  } catch { /* 已被 http 拦截器提示 */ }
}
function onLogout() {
  auth.logout()
  toast.success('已退出登录')
  router.push('/login')
}
onMounted(loadMe)
</script>
