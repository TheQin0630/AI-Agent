import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
  { path: '/', redirect: '/admin' },
  { path: '/admin', component: () => import('@/views/AdminDashboardView.vue'), meta: { auth: true, admin: true } },
  { path: '/users', component: () => import('@/views/UserManageView.vue'), meta: { auth: true, admin: true } },
  { path: '/tasks', component: () => import('@/views/AdminTaskListView.vue'), meta: { auth: true, admin: true } },
  { path: '/extractions', component: () => import('@/views/AdminExtractionListView.vue'), meta: { auth: true, admin: true } },
  { path: '/extractions/:id', component: () => import('@/views/AdminExtractionDetailView.vue'), meta: { auth: true, admin: true } },
  { path: '/risk-rules', component: () => import('@/views/RiskRuleView.vue'), meta: { auth: true, admin: true } },
  { path: '/llm-logs', component: () => import('@/views/LlmLogView.vue'), meta: { auth: true, admin: true } }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.guest && auth.token) return '/admin'
  if (!to.meta.auth) return
  if (!auth.token) return '/login'
  if (!auth.user) {
    try { const res: any = await authApi.me(); auth.setUser(res.data) } catch { return '/login' }
  }
  if (to.meta.admin && auth.user?.role !== 'ADMIN') return '/login'
})
export default router
