package com.example.contractagent.task;

import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonResultStore;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.DifyCreateExtractionRequest;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.dto.ComparisonTaskImportPayload;
import com.example.contractagent.user.User;
import com.example.contractagent.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonTaskImportServiceTest {
    @Mock ComparisonTaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @Mock ContractService contractService;
    @Mock ExtractionService extractionService;
    @Mock ComparisonService comparisonService;
    @Mock ComparisonResultStore comparisonResultStore;

    private ComparisonTaskImportService service;
    private final MockMultipartFile buyFile = new MockMultipartFile("buyFile", "buy.txt", "text/plain", "buy".getBytes());
    private final MockMultipartFile sellFile = new MockMultipartFile("sellFile", "sell.txt", "text/plain", "sell".getBytes());

    @BeforeEach
    void setUp() {
        service = new ComparisonTaskImportService(taskRepository, userRepository, contractService,
                extractionService, comparisonService, comparisonResultStore);
        ReflectionTestUtils.setField(service, "ownerUsername", "admin");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://example.test/");
    }

    @Test
    void importsCompleteTaskWithTwoBoundExtractions() {
        when(taskRepository.findBySource("FEISHU", "msg-1")).thenReturn(Optional.empty());
        User owner = new User(); owner.setId(7L); owner.setEnabled(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(owner));
        doAnswer(invocation -> { ((ComparisonTask) invocation.getArgument(0)).setId(1001L); return 1; })
                .when(taskRepository).insertIgnoreSource(any(ComparisonTask.class));
        Contract buy = new Contract(); buy.setId(11L);
        Contract sell = new Contract(); sell.setId(12L);
        when(contractService.saveForTask(1001L, ContractSide.BUY, buyFile)).thenReturn(buy);
        when(contractService.saveForTask(1001L, ContractSide.SELL, sellFile)).thenReturn(sell);
        Extraction buyExtraction = new Extraction();
        Extraction sellExtraction = new Extraction();
        when(extractionService.createFromDify(eq(11L), any(), isNull())).thenReturn(buyExtraction);
        when(extractionService.createFromDify(eq(12L), any(), isNull())).thenReturn(sellExtraction);
        ComparisonResult result = ComparisonResult.builder().differences(List.of()).riskLevel(RiskLevel.HIGH).build();
        when(comparisonService.compare(buyExtraction, sellExtraction)).thenReturn(result);
        when(comparisonResultStore.write(result)).thenReturn("{\"differences\":[]}");

        var response = service.importTask(payload(), buyFile, sellFile);

        assertThat(response.taskId()).isEqualTo(1001L);
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.created()).isTrue();
        assertThat(response.taskUrl()).isEqualTo("https://example.test/tasks/1001");
        verify(taskRepository).updateById(argThat((ComparisonTask task) -> task.getRiskLevel() == RiskLevel.HIGH
                && task.getStatus() == TaskStatus.DONE && "FEISHU".equals(task.getSourceType())));
    }

    @Test
    void duplicateEventReturnsExistingTaskWithoutWritingFiles() {
        ComparisonTask existing = new ComparisonTask();
        existing.setId(1001L); existing.setStatus(TaskStatus.DONE);
        when(taskRepository.findBySource("FEISHU", "msg-1")).thenReturn(Optional.of(existing));

        var response = service.importTask(payload(), buyFile, sellFile);

        assertThat(response.created()).isFalse();
        assertThat(response.taskId()).isEqualTo(1001L);
        verifyNoInteractions(userRepository, contractService, extractionService, comparisonService, comparisonResultStore);
        verify(taskRepository, never()).insertIgnoreSource(any(ComparisonTask.class));
    }

    @Test
    void concurrentDuplicateInsertReturnsWinningTaskWithoutWritingFiles() {
        ComparisonTask winner = new ComparisonTask();
        winner.setId(1002L); winner.setStatus(TaskStatus.DONE);
        when(taskRepository.findBySource("FEISHU", "msg-1"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        User owner = new User(); owner.setId(7L); owner.setEnabled(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(owner));
        when(taskRepository.insertIgnoreSource(any(ComparisonTask.class))).thenReturn(0);

        var response = service.importTask(payload(), buyFile, sellFile);

        assertThat(response.created()).isFalse();
        assertThat(response.taskId()).isEqualTo(1002L);
        verifyNoInteractions(contractService, extractionService, comparisonService, comparisonResultStore);
    }

    private ComparisonTaskImportPayload payload() {
        DifyCreateExtractionRequest fields = new DifyCreateExtractionRequest(
                "申请", null, null, "供应商", "商品", null, null,
                null, null, null, "CNY", null, null, null, null, null, null);
        return new ComparisonTaskImportPayload("飞书合同对比", "msg-1", "open-1", fields, fields, "存在风险");
    }
}
