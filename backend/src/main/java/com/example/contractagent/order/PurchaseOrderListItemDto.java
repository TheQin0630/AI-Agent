package com.example.contractagent.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseOrderListItemDto(
        Long id,
        String orderNo,
        String applicationNo,
        String status,
        String supplierName,
        String itemName,
        BigDecimal orderQuantity,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime createdAt
) {
    static PurchaseOrderListItemDto from(PurchaseOrder order) {
        return new PurchaseOrderListItemDto(
                order.getId(),
                order.getOrderNo(),
                order.getApplicationNo(),
                order.getStatus(),
                order.getSupplierName(),
                order.getItemName(),
                order.getOrderQuantity(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt()
        );
    }
}
