package com.example.contractagent.extraction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("extraction")
public class Extraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String supplierName;
    private String itemName;
    private String itemModel;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal purchaseUnitPrice;
    private BigDecimal purchaseTotalAmount;
    private LocalDate expectedDeliveryDate;
    private String shippingMethod;
    private String paymentTerms;
    private String deliveryLocation;
    private String applicationNo;
    private String applicationTitle;
    private LocalDate applyDate;
    private String applicationType;
    private String applicantName;
    private String currency;
    private String taxRateName;
    private String applicationStatus;
    private LocalDateTime createTime;
    private String message;
    private BigDecimal confidence;
    private String rawQuote;
    private LocalDateTime extractedAt;
    private String sourceJson;
    private String resultJson;
    private String sourceType;
    private String sourceEventId;
    private String sourceSenderId;
}
