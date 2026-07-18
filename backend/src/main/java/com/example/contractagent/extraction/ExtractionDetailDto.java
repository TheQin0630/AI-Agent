package com.example.contractagent.extraction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 申请单详情 DTO（用于 Dify 写入后的确认页面展示）
 */
public record ExtractionDetailDto(
        Long id,
        Long contractId,
        String applicationNo,
        String applicationStatus,
        String applicationTitle,
        String applicationType,
        String applicantName,
        LocalDate applyDate,
        String supplierName,
        String itemName,
        String itemModel,
        String unit,
        BigDecimal quantity,
        BigDecimal purchaseUnitPrice,
        BigDecimal purchaseTotalAmount,
        BigDecimal taxInclusiveAmount,
        String currency,
        String taxRateName,
        LocalDate expectedDeliveryDate,
        String shippingMethod,
        String deliveryLocation,
        String paymentTerms,
        String message,
        LocalDateTime createTime,
        LocalDateTime extractedAt,
        String sourceJson,
        String resultJson,
        ExtractionWorkflowDto workflow
) {
    public static ExtractionDetailDto from(Extraction e) {
        return new ExtractionDetailDto(
                e.getId(),
                e.getContractId(),
                e.getApplicationNo(),
                e.getApplicationStatus(),
                e.getApplicationTitle(),
                e.getApplicationType(),
                e.getApplicantName(),
                e.getApplyDate(),
                e.getSupplierName(),
                e.getItemName(),
                e.getItemModel(),
                e.getUnit(),
                e.getQuantity(),
                e.getPurchaseUnitPrice(),
                e.getPurchaseTotalAmount(),
                taxInclusiveAmount(e),
                e.getCurrency(),
                e.getTaxRateName(),
                e.getExpectedDeliveryDate(),
                e.getShippingMethod(),
                e.getDeliveryLocation(),
                e.getPaymentTerms(),
                e.getMessage(),
                e.getCreateTime(),
                e.getExtractedAt(),
                e.getSourceJson(),
                e.getResultJson(),
                ExtractionWorkflowDto.from(e.getApplicationStatus())
        );
    }

    private static BigDecimal taxInclusiveAmount(Extraction e) {
        if (e.getPurchaseTotalAmount() != null) {
            return e.getPurchaseTotalAmount();
        }
        if (e.getQuantity() == null || e.getPurchaseUnitPrice() == null) {
            return null;
        }
        return e.getQuantity().multiply(e.getPurchaseUnitPrice());
    }
}
