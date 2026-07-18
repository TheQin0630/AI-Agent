package com.example.contractagent.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long taskId;
    private Long extractionId;
    private String applicationNo;
    private Long productId;
    private String supplierName;
    private String itemName;
    private String itemModel;
    private String unit;
    private BigDecimal requestedQuantity;
    private BigDecimal inventoryQuantity;
    private BigDecimal orderQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String currency;
    private String taxRateName;
    private LocalDate expectedDeliveryDate;
    private String shippingMethod;
    private String deliveryLocation;
    private String paymentTerms;
    private String inventoryCheckStatus;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
}
