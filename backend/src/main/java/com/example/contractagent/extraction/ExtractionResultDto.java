package com.example.contractagent.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExtractionResultDto(
        @JsonProperty("supplier_name")            String supplierName,
        @JsonProperty("item_name")                String itemName,
        @JsonProperty("item_model")               String itemModel,
        @JsonProperty("unit")                     String unit,
        @JsonProperty("quantity")                 BigDecimal quantity,
        @JsonProperty("purchase_unit_price")      BigDecimal purchaseUnitPrice,
        @JsonProperty("purchase_total_amount")    BigDecimal purchaseTotalAmount,
        @JsonProperty("expected_delivery_date")   LocalDate expectedDeliveryDate,
        @JsonProperty("payment_terms")            String paymentTerms,
        @JsonProperty("delivery_location")        String deliveryLocation,
        @JsonProperty("raw_quote")                String rawQuote,
        @JsonProperty("confidence")               BigDecimal confidence
) {}
