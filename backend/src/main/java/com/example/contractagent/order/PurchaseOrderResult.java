package com.example.contractagent.order;

import java.math.BigDecimal;

public record PurchaseOrderResult(PurchaseOrder order,
                                  BigDecimal requestedQuantity,
                                  BigDecimal inventoryQuantity,
                                  boolean inventorySufficient) {
}
