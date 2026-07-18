import { http } from "./http";

export interface PurchaseOrderListItem {
  id: number; orderNo: string; applicationNo: string | null; status: string;
  supplierName: string; itemName: string; orderQuantity: string;
  totalAmount: string; currency: string; createdAt: string;
}
export interface PurchaseOrderDetail extends PurchaseOrderListItem {
  shippingMethod: string | null; expectedDeliveryDate: string | null;
  paymentTerms: string | null; itemModel: string | null; unit: string;
  unitPrice: string; taxInclusiveAmount: string; taxRateName: string | null;
}
export interface PurchaseOrderListResponse { total: number; items: PurchaseOrderListItem[]; }
export const purchaseOrderApi = {
  list: (params: { page?: number; size?: number }) =>
    http.get<unknown, { code: number; message: string; data: PurchaseOrderListResponse }>("/purchase-orders", { params }),
  get: (id: number) =>
    http.get<unknown, { code: number; message: string; data: PurchaseOrderDetail }>(`/purchase-orders/${id}`),
};
