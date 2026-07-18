package com.example.contractagent.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_inventory")
public class ProductInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String itemName;
    private String itemModel;
    private String itemModelKey;
    private String unit;
    private BigDecimal availableQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
