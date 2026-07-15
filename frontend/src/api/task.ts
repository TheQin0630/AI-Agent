import { http } from './http'
export const taskApi = {
  create: (form: FormData) => http.post('/tasks', form, { headers: { 'Content-Type': 'multipart/form-data' } }),
  list: (params: { page?: number; size?: number; status?: string }) => http.get('/tasks', { params }),
  detail: (id: number) => http.get(`/tasks/${id}`),
  chat: (id: number, content: string) => http.post(`/tasks/${id}/chat`, { content }),
  messages: (id: number, params?: { page?: number; size?: number }) => http.get(`/tasks/${id}/messages`, { params }),
  report: (id: number) => http.get(`/tasks/${id}/report`, { responseType: 'text' }),
  retry: (id: number) => http.post(`/tasks/${id}/retry`),
  confirm: (id: number) => http.post(`/tasks/${id}/confirm`)
}
