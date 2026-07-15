import { http } from './http'
export const authApi = {
  login: (data: { username: string; password: string }) => http.post('/auth/login', data),
  register: (data: { username: string; password: string }) => http.post('/auth/register', data),
  me: () => http.get('/auth/me')
}
