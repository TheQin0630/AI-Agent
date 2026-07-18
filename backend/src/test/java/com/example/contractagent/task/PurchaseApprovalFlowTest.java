package com.example.contractagent.task;

import com.example.contractagent.agent.AgentRunner;
import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonResultStore;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.order.PurchaseOrder;
import com.example.contractagent.order.PurchaseOrderResult;
import com.example.contractagent.order.PurchaseOrderService;
import com.example.contractagent.supplement.SupplementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PurchaseApprovalFlowTest {
    private final ComparisonTaskRepository taskRepository = mock(ComparisonTaskRepository.class);
    private final ContractService contractService = mock(ContractService.class);
    private final ExtractionService extractionService = mock(ExtractionService.class);
    private final ComparisonService comparisonService = mock(ComparisonService.class);
    private final ComparisonResultStore resultStore = mock(ComparisonResultStore.class);
    private final PurchaseOrderService orderService = mock(PurchaseOrderService.class);
    private ComparisonTaskService service;

    @BeforeEach
    void setUp() {
        service = new ComparisonTaskService(
                taskRepository,
                contractService,
                mock(AgentRunner.class),
                extractionService,
                comparisonService,
                resultStore,
                orderService,
                mock(SupplementService.class)
        );
    }

    @Test
    void taskApprovalCreatesPendingApplicationButDoesNotCreateOrder() {
        ComparisonTask task = task(TaskStatus.DONE);
        Contract buy = contract(20L, 10L, ContractSide.BUY);
        Contract sell = contract(21L, 10L, ContractSide.SELL);
        Extraction buyExtraction = extraction(30L, 20L, null);
        Extraction sellExtraction = extraction(31L, 21L, null);
        Extraction prepared = extraction(30L, 20L, "待确认");
        prepared.setApplicationNo("PA-001");
        ComparisonResult comparison = mock(ComparisonResult.class);

        when(taskRepository.selectById(10L)).thenReturn(task);
        when(contractService.requireForTask(10L, ContractSide.BUY)).thenReturn(buy);
        when(contractService.requireForTask(10L, ContractSide.SELL)).thenReturn(sell);
        when(extractionService.getByContractId(20L)).thenReturn(buyExtraction);
        when(extractionService.getByContractId(21L)).thenReturn(sellExtraction);
        when(comparisonService.compare(buyExtraction, sellExtraction)).thenReturn(comparison);
        when(resultStore.write(comparison)).thenReturn("{}");
        when(extractionService.prepareForConfirmation(30L)).thenReturn(prepared);

        var response = service.confirmPurchase(1L, 10L);

        assertThat(response.status()).isEqualTo(TaskStatus.CONFIRMED);
        assertThat(response.applicationStatus()).isEqualTo("待确认");
        assertThat(response.orderId()).isNull();
        verify(orderService, never()).createOrGet(anyLong(), anyLong(), any());
        verify(extractionService).prepareForConfirmation(30L);
    }

    @Test
    void applicationConfirmationCreatesFormalOrderForInventoryShortage() {
        ComparisonTask task = task(TaskStatus.CONFIRMED);
        Contract buy = contract(20L, 10L, ContractSide.BUY);
        Extraction application = extraction(30L, 20L, "待确认");
        application.setApplicationNo("PA-001");
        PurchaseOrder order = new PurchaseOrder();
        order.setId(40L);
        order.setOrderNo("PO-001");
        order.setStatus("SUBMITTED");
        order.setOrderQuantity(new BigDecimal("20"));
        order.setTotalAmount(new BigDecimal("2000"));
        PurchaseOrderResult orderResult = new PurchaseOrderResult(
                order, new BigDecimal("20"), BigDecimal.ZERO, false);

        when(extractionService.getById(30L)).thenReturn(application);
        when(contractService.getById(20L)).thenReturn(buy);
        when(taskRepository.selectById(10L)).thenReturn(task);
        when(orderService.createOrGet(10L, 1L, application)).thenReturn(orderResult);

        var response = service.confirmApplication(1L, 30L);

        assertThat(response.orderNo()).isEqualTo("PO-001");
        assertThat(response.applicationStatus()).isEqualTo("已确认");
        verify(extractionService).updateApplicationStatus(
                30L, "已确认", "采购申请已确认，正式采购订单已创建并提交");
    }

    @Test
    void repeatedApplicationConfirmationReturnsExistingOrderWithoutCreatingAgain() {
        ComparisonTask task = task(TaskStatus.CONFIRMED);
        Contract buy = contract(20L, 10L, ContractSide.BUY);
        Extraction application = extraction(30L, 20L, "已确认");
        application.setApplicationNo("PA-001");
        PurchaseOrder order = new PurchaseOrder();
        order.setId(40L);
        order.setOrderNo("PO-001");
        order.setStatus("SUBMITTED");
        PurchaseOrderResult existing = new PurchaseOrderResult(
                order, new BigDecimal("20"), BigDecimal.ZERO, false);

        when(extractionService.getById(30L)).thenReturn(application);
        when(contractService.getById(20L)).thenReturn(buy);
        when(taskRepository.selectById(10L)).thenReturn(task);
        when(orderService.getConfirmedResult(10L, application)).thenReturn(existing);

        var response = service.confirmApplication(1L, 30L);

        assertThat(response.orderNo()).isEqualTo("PO-001");
        verify(orderService, never()).createOrGet(anyLong(), anyLong(), any());
        verify(extractionService, never()).updateApplicationStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void standaloneDifyApplicationConfirmationCreatesFormalOrder() {
        Extraction application = extraction(30L, null, "待确认");
        application.setApplicationNo("PA-DIFY-001");
        PurchaseOrder order = new PurchaseOrder();
        order.setId(41L);
        order.setOrderNo("PO-20260719-E000030");
        order.setStatus("SUBMITTED");
        order.setOrderQuantity(new BigDecimal("20"));
        order.setTotalAmount(new BigDecimal("2000"));
        PurchaseOrderResult orderResult = new PurchaseOrderResult(
                order, new BigDecimal("20"), BigDecimal.ZERO, false);
        when(extractionService.getById(30L)).thenReturn(application);
        when(orderService.createOrGet(isNull(), eq(1L), same(application))).thenReturn(orderResult);

        var response = service.confirmApplication(1L, 30L);

        assertThat(response.id()).isNull();
        assertThat(response.orderNo()).isEqualTo("PO-20260719-E000030");
        assertThat(response.applicationStatus()).isEqualTo("已确认");
        verify(extractionService).updateApplicationStatus(
                30L, "已确认", "采购申请已确认，正式采购订单已创建并提交");
    }

    private ComparisonTask task(TaskStatus status) {
        ComparisonTask task = new ComparisonTask();
        task.setId(10L);
        task.setUserId(1L);
        task.setStatus(status);
        return task;
    }

    private Contract contract(Long id, Long taskId, ContractSide side) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setTaskId(taskId);
        contract.setSide(side);
        return contract;
    }

    private Extraction extraction(Long id, Long contractId, String status) {
        Extraction extraction = new Extraction();
        extraction.setId(id);
        extraction.setContractId(contractId);
        extraction.setApplicationStatus(status);
        return extraction;
    }
}
