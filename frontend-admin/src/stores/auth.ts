import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null as null | { id: number; username: string; role: 'USER' | 'ADMIN' }
  }),
  getters: {
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    setToken(t: string) { this.token = t; localStorage.setItem('token', t) },
    setUser(u: any) { this.user = u },
    logout() { this.token = ''; this.user = null; localStorage.removeItem('token') }
  }
})
