<template>
  <main class="login-page flex items-center justify-center min-h-screen px-4">
    <div class="login-shell w-full max-w-sm">
      <!-- Brand -->
      <div class="flex items-center gap-3 mb-8 justify-center">
        <div class="w-10 h-10 grid place-items-center font-bold text-sm"
          :style="`background:var(--sidebar-primary);color:var(--sidebar-primary-foreground);border-radius:calc(var(--radius) + 4px)`">CC</div>
        <div>
          <h1 class="text-base font-semibold leading-tight" style="color:var(--foreground)">合同对比 Agent</h1>
          <p class="text-xs" style="color:var(--muted-foreground)">Contract Compare Platform</p>
        </div>
      </div>
      <!-- Card -->
      <div class="login-card">
        <div class="flex gap-0 mb-6" style="border-bottom:1px solid var(--border)">
          <button v-for="t in tabs" :key="t.key" @click="tab=t.key"
            class="pb-3 text-sm font-semibold cursor-pointer bg-transparent border-0"
            :style="tabStyle(t.key)">{{ t.label }}</button>
        </div>
        <!-- Login Form -->
        <Transition name="form-swap" mode="out-in">
        <form v-if="tab==='login'" key="login" @submit.prevent="onLogin" class="flex flex-col gap-4">
          <FieldInput v-model="form.username" label="用户名" :icon="User" placeholder="请输入用户名" />
          <FieldInput v-model="form.password" label="密码" :icon="Lock" type="password" placeholder="请输入密码" />
          <button type="submit" :disabled="loading"
            class="submit-button mt-2 flex items-center justify-center gap-2 cursor-pointer font-medium text-sm disabled:opacity-60">登录</button>
        </form>
        <!-- Register Form -->
        <form v-else key="register" @submit.prevent="onRegister" class="flex flex-col gap-4">
          <FieldInput v-model="form.username" label="用户名" :icon="User" placeholder="请输入用户名" />
          <FieldInput v-model="form.password" label="密码" :icon="Lock" type="password" placeholder="请输入密码" />
          <FieldInput v-model="form.confirm" label="确认密码" :icon="Lock" type="password" placeholder="请再次输入密码" />
          <button type="submit" :disabled="loading"
            class="submit-button secondary mt-2 flex items-center justify-center gap-2 cursor-pointer font-medium text-sm disabled:opacity-60">注册</button>
        </form>
        </Transition>
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
    auth.setToken(res.data.token); auth.setUser(res.data.user)
    toast.success('登录成功')
    // 支持 redirect 跳回（Dify 确认链接场景）
    const redirect = (router.currentRoute.value.query.redirect as string) || '/tasks'
    router.push(redirect)
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
<style scoped>
.login-page{position:relative;overflow:hidden;background:radial-gradient(circle at 50% 15%,#e8f2ff 0,rgba(232,242,255,0) 34%),var(--background)}
.login-page::before,.login-page::after{content:"";position:absolute;border-radius:999px;filter:blur(2px);pointer-events:none;animation:float 8s ease-in-out infinite}
.login-page::before{width:260px;height:260px;left:-100px;bottom:-90px;background:rgba(23,105,224,.055)}
.login-page::after{width:180px;height:180px;right:-65px;top:8%;background:rgba(14,165,233,.05);animation-delay:-3s}
.login-shell{position:relative;z-index:1;animation:shell-in .55s cubic-bezier(.2,.8,.2,1) both}
.login-card{padding:28px;background:rgba(255,255,255,.92);border:1px solid rgba(218,225,235,.88);border-radius:24px;box-shadow:0 24px 70px rgba(24,52,84,.11),0 3px 12px rgba(24,52,84,.05);backdrop-filter:blur(18px)}
.submit-button{height:42px;padding:0 18px;border:1px solid #c9ddfb;border-radius:14px;background:#e8f1ff;color:#155fc9;box-shadow:0 7px 16px rgba(23,105,224,.1);transition:transform .22s cubic-bezier(.2,.8,.2,1),box-shadow .22s ease,background .22s ease}
.submit-button:hover{transform:translateY(-2px);background:#dceaff;box-shadow:0 10px 20px rgba(23,105,224,.13)}
.submit-button:active{transform:translateY(0) scale(.985)}
.submit-button.secondary{background:#eef5ff;color:#155fc9;border-color:#d4e4fb;box-shadow:none}
.form-swap-enter-active,.form-swap-leave-active{transition:opacity .2s ease,transform .24s cubic-bezier(.2,.8,.2,1)}
.form-swap-enter-from{opacity:0;transform:translateX(10px)}
.form-swap-leave-to{opacity:0;transform:translateX(-8px)}
@keyframes shell-in{from{opacity:0;transform:translateY(14px) scale(.985)}}
@keyframes float{50%{transform:translateY(-14px) scale(1.04)}}
</style>
