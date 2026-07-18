import { http } from "./http";
import type { WorkflowState } from "./task";

export interface ExtractionDetail {
  id: number;
  contractId: number | null;
  applicationNo: string;
  applicationStatus: string;
  applicationTitle: string | null;
  applicationType: string | null;
  applicantName: string | null;
  applyDate: string | null;
  supplierName: string | null;
  itemName: string | null;
  itemModel: string | null;
  unit: string | null;
  quantity: string | null;
  purchaseUnitPrice: string | null;
  purchaseTotalAmount: string | null;
  taxInclusiveAmount: string | null;
  currency: string | null;
  taxRateName: string | null;
  expectedDeliveryDate: string | null;
  shippingMethod: string | null;
  deliveryLocation: string | null;
  paymentTerms: string | null;
  message: string | null;
  createTime: string | null;
  extractedAt: string | null;
  sourceJson: string | null;
  resultJson: string | null;
  workflow?: WorkflowState;
}

export interface ExtractionListItem {
  id: number;
  applicationNo: string;
  applicationTitle: string | null;
  applicationStatus: string;
  supplierName: string | null;
  itemName: string | null;
  applicationType: string | null;
  createTime: string | null;
  extractedAt: string | null;
  workflow?: WorkflowState;
}

export interface ExtractionListResponse {
  total: number;
  items: ExtractionListItem[];
}

export interface ConfirmApplicationResult {
  id: number | null;
  status: string | null;
  applicationNo: string | null;
  applicationStatus: string;
  confirmedAt: string | null;
  orderId: number | null;
  orderNo: string | null;
  orderStatus: string | null;
  orderQuantity: string | null;
  totalAmount: string | null;
  inventoryCheckStatus: string;
  message: string;
}

export const extractionApi = {
  /** 分页查询申请单列表 */
  list(params: { page?: number; size?: number; status?: string }) {
    return http.get<
      unknown,
      { code: number; message: string; data: ExtractionListResponse }
    >("/extractions", { params });
  },
  /** 获取申请单详情 */
  get(id: number) {
    return http.get<
      unknown,
      { code: number; message: string; data: ExtractionDetail }
    >(`/extractions/${id}`);
  },
  /** 确认申请单（待确认 → 已确认） */
  confirm(id: number) {
    return http.patch<
      unknown,
      { code: number; message: string; data: ConfirmApplicationResult }
    >(`/extractions/${id}/confirm`);
  },
  reject(id: number, reason: string) {
    return http.patch<
      unknown,
      { code: number; message: string; data: { id: number } }
    >(`/extractions/${id}/reject`, { reason });
  },
};
