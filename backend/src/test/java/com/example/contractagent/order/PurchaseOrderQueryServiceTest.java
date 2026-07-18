package com.example.contractagent.order;

import com.example.contractagent.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseOrderQueryServiceTest {
    private final PurchaseOrderRepository repository = mock(PurchaseOrderRepository.class);
    private final PurchaseOrderQueryService service = new PurchaseOrderQueryService(repository);

    @Test
    void returnsOnlyCurrentUsersOrders() {
        PurchaseOrder order = order();
        when(repository.countByUser(7L)).thenReturn(1L);
        when(repository.listByUser(7L, 0, 20)).thenReturn(List.of(order));

        PurchaseOrderListResponse response = service.list(7L, 1, 20);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement()
                .extracting(PurchaseOrderListItemDto::orderNo)
                .isEqualTo("PO-001");
    }

    @Test
    void hidesOrderOwnedByAnotherUser() {
        when(repository.findOwnedById(9L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(7L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购订单不存在");
    }

    private PurchaseOrder order() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setOrderNo("PO-001");
        order.setApplicationNo("PA-001");
        order.setSupplierName("供应商");
        order.setItemName("商品");
        order.setOrderQuantity(BigDecimal.ONE);
        order.setTotalAmount(BigDecimal.TEN);
        order.setCurrency("CNY");
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
