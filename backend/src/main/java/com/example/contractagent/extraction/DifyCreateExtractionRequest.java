package com.example.contractagent.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dify 工作流通过 HTTP 请求节点创建采购申请单的入参。
 * 接受 snake_case JSON（LLM 产出格式），忽略未知字段。
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DifyCreateExtractionRequest(
        String applicationTitle,
        String applicationType,
        LocalDate applyDate,
        String supplierName,
        String itemName,
        String itemModel,
        String unit,
        BigDecimal quantity,
        BigDecimal purchaseUnitPrice,
        @JsonAlias({"purchaseMoneyTax", "purchase_money_tax"}) BigDecimal purchaseTotalAmount,
        @JsonAlias({"currencyName", "currency_name"}) String currency,
        LocalDate expectedDeliveryDate,
        @JsonAlias({"sendMode", "send_mode", "delivery_method", "shipping_method"}) String shippingMethod,
        String deliveryLocation,
        String paymentTerms,
        @JsonAlias({"applicantName", "buyer_name", "applicant_name"}) String applicantName,
        @JsonAlias({"taxRateName", "tax_rate_name", "tax_rate"}) String taxRateName
) {
}
