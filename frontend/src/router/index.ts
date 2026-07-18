import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
  { path: '/', redirect: '/tasks' },
  { path: '/tasks', component: () => import('@/views/TaskListView.vue'), meta: { auth: true } },
  { path: '/tasks/new', component: () => import('@/views/TaskCreateView.vue'), meta: { auth: true } },
  { path: '/tasks/:id', component: () => import('@/views/TaskDetailView.vue'), meta: { auth: true } },
  { path: '/extractions', component: () => import('@/views/ExtractionListView.vue'), meta: { auth: true } },
  { path: '/extractions/:id/confirm', component: () => import('@/views/ExtractionConfirmView.vue'), meta: { auth: true } },
  { path: '/purchase-orders', component: () => import('@/views/PurchaseOrderListView.vue'), meta: { auth: true } },
  { path: '/purchase-orders/:id', component: () => import('@/views/PurchaseOrderDetailView.vue'), meta: { auth: true } },
  { path: '/supplements/:id', component: () => import('@/views/SupplementView.vue'), meta: { auth: true } },
  { path: '/me', component: () => import('@/views/MeView.vue'), meta: { auth: true } }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.token) {
    // 记住目标路径，登录后跳回（支持 Dify 确认链接场景）
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.token) return '/tasks'
})
export default router
