import { defineStore } from 'pinia'

export type ToastType = 'success' | 'error' | 'warning' | 'info'
export interface ToastItem { id: number; type: ToastType; message: string }

let _id = 0
export const useToastStore = defineStore('toast', {
  state: () => ({ items: [] as ToastItem[] }),
  actions: {
    push(message: string, type: ToastType = 'info', ttl = 2600) {
      const id = ++_id
      this.items.push({ id, type, message })
      setTimeout(() => this.dismiss(id), ttl)
    },
    success(m: string) { this.push(m, 'success') },
    error(m: string) { this.push(m, 'error', 3600) },
    warning(m: string) { this.push(m, 'warning') },
    info(m: string) { this.push(m, 'info') },
    dismiss(id: number) {
      const i = this.items.findIndex(t => t.id === id)
      if (i >= 0) this.items.splice(i, 1)
    }
  }
})
