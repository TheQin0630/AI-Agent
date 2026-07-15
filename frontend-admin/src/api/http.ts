import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import router from '@/router'

export const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use(cfg => {
  const auth = useAuthStore()
  if (auth.token) cfg.headers.Authorization = `Bearer ${auth.token}`
  return cfg
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      useToastStore().error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body
  },
  err => {
    const body = err.response?.data
    if (body?.code === 1002) {
      useAuthStore().logout()
      router.push('/login')
    }
    useToastStore().error(body?.message || err.message || '网络异常')
    return Promise.reject(err)
  }
)
