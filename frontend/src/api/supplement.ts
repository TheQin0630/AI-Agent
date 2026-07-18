import { http } from './http'

export type SupplementStatus = 'PENDING' | 'SUBMITTED' | 'RESOLVED'

export interface SupplementDetail {
  id: number
  businessId: string
  supplementType: string
  supplementContent: string
  status: SupplementStatus
  submittedContent: string | null
  createdAt: string
}

export const supplementApi = {
  get(id: number) {
    return http.get<unknown, { code: number; message: string; data: SupplementDetail }>(`/supplements/${id}`)
  },
  submit(id: number, content: string) {
    return http.post<unknown, { code: number; message: string; data: SupplementDetail }>(`/supplements/${id}/submit`, { content })
  }
}
