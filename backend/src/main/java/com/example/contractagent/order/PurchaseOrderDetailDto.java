package com.example.contractagent.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderDetailDto(
        Long id,
        String orderNo,
        String applicationNo,
        String status,
        LocalDateTime createdAt,
        String supplierName,
        String shippingMethod,
        LocalDate expectedDeliveryDate,
        String paymentTerms,
        String itemName,
        String itemModel,
        String unit,
        BigDecimal unitPrice,
        BigDecimal orderQuantity,
        String currency,
        BigDecimal taxInclusiveAmount,
        String taxRateName
) {
    static PurchaseOrderDetailDto from(PurchaseOrder order) {
        return new PurchaseOrderDetailDto(
                order.getId(),
                order.getOrderNo(),
                order.getApplicationNo(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getSupplierName(),
                order.getShippingMethod(),
                order.getExpectedDeliveryDate(),
                order.getPaymentTerms(),
                order.getItemName(),
                order.getItemModel(),
                order.getUnit(),
                order.getUnitPrice(),
                order.getOrderQuantity(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getTaxRateName()
        );
    }
}
