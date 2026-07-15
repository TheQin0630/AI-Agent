import { http } from './http'

export interface ExtractionDetail {
  id: number
  contractId: number | null
  applicationNo: string
  applicationStatus: string
  applicationTitle: string | null
  applicationType: string | null
  applyDate: string | null
  supplierName: string | null
  itemName: string | null
  itemModel: string | null
  unit: string | null
  quantity: string | null
  purchaseUnitPrice: string | null
  purchaseTotalAmount: string | null
  currency: string | null
  expectedDeliveryDate: string | null
  deliveryLocation: string | null
  paymentTerms: string | null
  message: string | null
  createTime: string | null
  extractedAt: string | null
  sourceJson: string | null
  resultJson: string | null
}

export interface ExtractionListItem {
  id: number
  applicationNo: string
  applicationTitle: string | null
  applicationStatus: string
  supplierName: string | null
  itemName: string | null
  applicationType: string | null
  createTime: string | null
  extractedAt: string | null
}

export interface ExtractionListResponse {
  total: number
  items: ExtractionListItem[]
}

export const extractionApi = {
  list(params: { page?: number; size?: number; status?: string }) {
    return http.get<unknown, { code: number; message: string; data: ExtractionListResponse }>(
      '/extractions',
      { params }
    )
  },
  get(id: number) {
    return http.get<unknown, { code: number; message: string; data: ExtractionDetail }>(
      `/extractions/${id}`
    )
  },
  confirm(id: number) {
    return http.patch<unknown, { code: number; message: string; data: null }>(
      `/extractions/${id}/confirm`
    )
  }
}
