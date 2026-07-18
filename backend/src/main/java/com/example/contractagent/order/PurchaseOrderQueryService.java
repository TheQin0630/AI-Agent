package com.example.contractagent.order;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PurchaseOrderQueryService {
    private final PurchaseOrderRepository orderRepository;

    public PurchaseOrderListResponse list(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        return new PurchaseOrderListResponse(
                orderRepository.countByUser(userId),
                orderRepository.listByUser(userId, offset, safeSize).stream()
                        .map(PurchaseOrderListItemDto::from)
                        .toList()
        );
    }

    public PurchaseOrderDetailDto detail(Long userId, Long id) {
        PurchaseOrder order = orderRepository.findOwnedById(id, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND, "采购订单不存在"));
        return PurchaseOrderDetailDto.from(order);
    }
}
