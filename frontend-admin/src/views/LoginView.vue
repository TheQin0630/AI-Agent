<template>
  <main class="flex items-center justify-center min-h-screen px-4">
    <div class="w-full max-w-sm">
      <!-- Brand -->
      <div class="flex items-center gap-3 mb-8 justify-center">
        <div class="w-10 h-10 grid place-items-center font-bold text-sm"
          :style="`background:var(--sidebar-primary);color:var(--sidebar-primary-foreground);border-radius:calc(var(--radius) + 4px)`">CC</div>
        <div>
          <h1 class="text-base font-semibold leading-tight" style="color:var(--foreground)">合同对比 Agent</h1>
          <p class="text-xs" style="color:var(--muted-foreground)">管理端 · Admin Console</p>
        </div>
      </div>
      <!-- Card -->
      <div class="rounded-lg" :style="`background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:calc(var(--spacing)*6)`">
        <div class="flex gap-0 mb-6" style="border-bottom:1px solid var(--border)">
          <button v-for="t in tabs" :key="t.key" @click="tab=t.key"
            class="pb-3 text-sm font-semibold cursor-pointer bg-transparent border-0"
            :style="tabStyle(t.key)">{{ t.label }}</button>
        </div>
        <!-- Login Form -->
        <form v-if="tab==='login'" @submit.prevent="onLogin" class="flex flex-col gap-4">
          <FieldInput v-model="form.username" label="用户名" :icon="User" placeholder="请输入用户名" />
          <FieldInput v-model="form.password" label="密码" :icon="Lock" type="password" placeholder="请输入密码" />
          <button type="submit" :disabled="loading"
            class="mt-2 flex items-center justify-center gap-2 cursor-pointer font-medium text-sm disabled:opacity-60"
            style="height:32px;padding:0 calc(var(--spacing)*4);background:var(--primary);color:var(--primary-foreground);border:1px solid transparent;border-radius:var(--radius)">登录</button>
        </form>
        <!-- Register Form -->
        <form v-else @submit.prevent="onRegister" class="flex flex-col gap-4">
          <FieldInput v-model="form.username" label="用户名" :icon="User" placeholder="请输入用户名" />
          <FieldInput v-model="form.password" label="密码" :icon="Lock" type="password" placeholder="请输入密码" />
          <FieldInput v-model="form.confirm" label="确认密码" :icon="Lock" type="password" placeholder="请再次输入密码" />
          <button type="submit" :disabled="loading"
            class="mt-2 flex items-center justify-center gap-2 cursor-pointer font-medium text-sm disabled:opacity-60"
            style="height:32px;padding:0 calc(var(--spacing)*4);background:var(--secondary);color:var(--secondary-foreground);border:1px solid var(--border);border-radius:var(--radius)">注册</button>
        </form>
      </div>
    </div>
  </main>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from 'lucide-vue-next'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import FieldInput from '@/components/FieldInput.vue'

const tabs = [{ key: 'login', label: '登录' }, { key: 'register', label: '注册' }] as const
const tab = ref<'login' | 'register'>('login')
const form = reactive({ username: '', password: '', confirm: '' })
const loading = ref(false)
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
function tabStyle(k: string) {
  return k === tab.value
    ? 'color:var(--primary);border-bottom:2px solid var(--primary);margin-bottom:-1px'
    : 'color:var(--muted-foreground);border-bottom:2px solid transparent;margin-bottom:-1px;margin-left:calc(var(--spacing)*4)'
}
async function onLogin() {
  if (form.username.length < 3 || form.password.length < 6) return toast.warning('用户名≥3，密码≥6')
  loading.value = true
  try {
    const res: any = await authApi.login({ username: form.username, password: form.password })
    const user = res.data.user
    if (user.role !== 'ADMIN') {
      toast.error('无管理端权限')
      auth.logout()
      return
    }
    auth.setToken(res.data.token); auth.setUser(user)
    toast.success('登录成功')
    router.push('/admin')
  } finally { loading.value = false }
}
async function onRegister() {
  if (form.username.length < 3 || form.password.length < 6) return toast.warning('用户名≥3，密码≥6')
  if (form.password !== form.confirm) return toast.warning('两次密码不一致')
  loading.value = true
  try {
    await authApi.register({ username: form.username, password: form.password })
    toast.success('注册成功，请登录')
    tab.value = 'login'
  } finally { loading.value = false }
}
</script>
