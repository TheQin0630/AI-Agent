package com.example.contractagent.task;

import com.example.contractagent.agent.AgentRunner;
import com.example.contractagent.comparison.ComparisonResultStore;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.order.PurchaseOrderService;
import com.example.contractagent.supplement.SupplementRequest;
import com.example.contractagent.supplement.SupplementService;
import com.example.contractagent.supplement.SupplementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ComparisonTaskRejectServiceTest {
    private final ComparisonTaskRepository taskRepository = mock(ComparisonTaskRepository.class);
    private final ContractService contractService = mock(ContractService.class);
    private final ExtractionService extractionService = mock(ExtractionService.class);
    private final SupplementService supplementService = mock(SupplementService.class);
    private ComparisonTaskService service;

    @BeforeEach
    void setUp() {
        service = new ComparisonTaskService(taskRepository, contractService, mock(AgentRunner.class),
                extractionService, mock(ComparisonService.class), mock(ComparisonResultStore.class),
                mock(PurchaseOrderService.class), supplementService);
    }

    @Test
    void rejectsDoneTaskAndCreatesOneSupplementNotification() {
        ComparisonTask task = task(TaskStatus.DONE);
        Contract buy = new Contract();
        buy.setId(20L);
        Extraction extraction = new Extraction();
        extraction.setId(30L);
        SupplementRequest supplement = supplement();
        when(taskRepository.selectById(10L)).thenReturn(task);
        when(contractService.requireForTask(10L, ContractSide.BUY)).thenReturn(buy);
        when(extractionService.getByContractId(20L)).thenReturn(extraction);
        when(supplementService.createOrGet(any())).thenReturn(supplement);

        var response = service.rejectForSupplement(1L, 10L, "缺少盖章页");

        assertThat(response.status()).isEqualTo(TaskStatus.NEEDS_SUPPLEMENT);
        assertThat(response.supplement().id()).isEqualTo(40L);
        verify(extractionService).updateApplicationStatus(30L, "待补充", "审批驳回：缺少盖章页");
        verify(taskRepository).updateById(task);
    }

    @Test
    void repeatedRejectReturnsExistingSupplementWithoutDuplicating() {
        ComparisonTask task = task(TaskStatus.NEEDS_SUPPLEMENT);
        SupplementRequest supplement = supplement();
        when(taskRepository.selectById(10L)).thenReturn(task);
        when(supplementService.findLatestForBusiness("task:10", "REJECTED")).thenReturn(supplement);

        var response = service.rejectForSupplement(1L, 10L, "重复点击");

        assertThat(response.supplement().id()).isEqualTo(40L);
        verify(supplementService, never()).createOrGet(any());
    }

    private ComparisonTask task(TaskStatus status) {
        ComparisonTask task = new ComparisonTask();
        task.setId(10L);
        task.setUserId(1L);
        task.setTitle("采购合同复核");
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.of(2026, 7, 16, 18, 0));
        task.setSourceSenderId("ou_test");
        return task;
    }

    private SupplementRequest supplement() {
        SupplementRequest request = new SupplementRequest();
        request.setId(40L);
        request.setBusinessId("task:10");
        request.setSupplementType("REJECTED");
        request.setSupplementContent("缺少盖章页");
        request.setStatus(SupplementStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }
}
