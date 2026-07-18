package com.example.contractagent.order;

import com.example.contractagent.extraction.Extraction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseOrderServiceTest {
    private final ProductInventoryRepository inventoryRepository = mock(ProductInventoryRepository.class);
    private final PurchaseOrderRepository orderRepository = mock(PurchaseOrderRepository.class);
    private final PurchaseOrderService service = new PurchaseOrderService(inventoryRepository, orderRepository);

    @Test
    void createsProductAndSubmittedOrderForFullShortage() {
        when(orderRepository.findByTaskId(7L)).thenReturn(Optional.empty());
        when(inventoryRepository.findForUpdate("智能网关", "pro", "台")).thenReturn(null);
        doAnswer(invocation -> {
            ProductInventory product = invocation.getArgument(0);
            product.setId(11L);
            return 1;
        }).when(inventoryRepository).insert(any(ProductInventory.class));
        doAnswer(invocation -> {
            PurchaseOrder order = invocation.getArgument(0);
            order.setId(21L);
            return 1;
        }).when(orderRepository).insert(any(PurchaseOrder.class));

        PurchaseOrderResult result = service.createOrGet(7L, 1L, extraction("200", "500"));

        assertThat(result.inventorySufficient()).isFalse();
        assertThat(result.order().getOrderQuantity()).isEqualByComparingTo("200");
        assertThat(result.order().getTotalAmount()).isEqualByComparingTo("100000.00");
        assertThat(result.order().getStatus()).isEqualTo("SUBMITTED");
        assertThat(result.order().getOrderNo()).endsWith("000007");
    }

    @Test
    void skipsOrderWhenInventoryCoversDemand() {
        when(orderRepository.findByTaskId(8L)).thenReturn(Optional.empty());
        ProductInventory product = new ProductInventory();
        product.setId(12L);
        product.setAvailableQuantity(new BigDecimal("250"));
        when(inventoryRepository.findForUpdate("智能网关", "pro", "台")).thenReturn(product);

        PurchaseOrderResult result = service.createOrGet(8L, 1L, extraction("200", "500"));

        assertThat(result.inventorySufficient()).isTrue();
        assertThat(result.order()).isNull();
        verify(orderRepository, never()).insert(any(PurchaseOrder.class));
    }

    @Test
    void purchasesOnlyInventoryShortage() {
        when(orderRepository.findByTaskId(9L)).thenReturn(Optional.empty());
        ProductInventory product = new ProductInventory();
        product.setId(13L);
        product.setAvailableQuantity(new BigDecimal("80"));
        when(inventoryRepository.findForUpdate("智能网关", "pro", "台")).thenReturn(product);

        service.createOrGet(9L, 1L, extraction("200", "500"));

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(orderRepository).insert(captor.capture());
        assertThat(captor.getValue().getOrderQuantity()).isEqualByComparingTo("120");
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("60000.00");
    }

    @Test
    void createsIdempotentOrderForStandaloneDifyApplication() {
        Extraction extraction = extraction("20", "500");
        extraction.setId(77L);
        extraction.setApplicationNo("PA-DIFY-77");
        when(orderRepository.findByExtractionId(77L)).thenReturn(Optional.empty());
        when(inventoryRepository.findForUpdate("智能网关", "pro", "台")).thenReturn(null);
        doAnswer(invocation -> {
            ProductInventory product = invocation.getArgument(0);
            product.setId(15L);
            return 1;
        }).when(inventoryRepository).insert(any(ProductInventory.class));

        PurchaseOrderResult result = service.createOrGet(null, 1L, extraction);

        assertThat(result.order().getTaskId()).isNull();
        assertThat(result.order().getExtractionId()).isEqualTo(77L);
        assertThat(result.order().getOrderNo()).endsWith("E000077");
        verify(orderRepository).insert(result.order());
    }

    private Extraction extraction(String quantity, String unitPrice) {
        Extraction extraction = new Extraction();
        extraction.setSupplierName("YY电子科技");
        extraction.setItemName("智能网关");
        extraction.setItemModel("Pro");
        extraction.setUnit("台");
        extraction.setQuantity(new BigDecimal(quantity));
        extraction.setPurchaseUnitPrice(new BigDecimal(unitPrice));
        extraction.setCurrency("CNY");
        return extraction;
    }
}
