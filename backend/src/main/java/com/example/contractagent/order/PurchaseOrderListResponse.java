package com.example.contractagent.order;

import java.util.List;

public record PurchaseOrderListResponse(long total, List<PurchaseOrderListItemDto> items) {
}
