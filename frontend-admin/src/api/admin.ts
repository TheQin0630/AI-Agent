import { http } from './http'
export const adminApi = {
  stats: () => http.get('/admin/stats'),
  listUsers: (params: any) => http.get('/admin/users', { params }),
  updateUser: (id: number, body: any) => http.patch(`/admin/users/${id}`, body),
  listTasks: (params: any) => http.get('/admin/tasks', { params }),
  taskDetail: (id: number) => http.get(`/admin/tasks/${id}`),
  listRiskRules: () => http.get('/admin/risk-rules'),
  createRiskRule: (body: any) => http.post('/admin/risk-rules', body),
  updateRiskRule: (id: number, body: any) => http.patch(`/admin/risk-rules/${id}`, body),
  deleteRiskRule: (id: number) => http.delete(`/admin/risk-rules/${id}`),
  listLlmLogs: (params: any) => http.get('/admin/llm-logs', { params })
}
