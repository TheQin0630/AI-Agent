package com.example.contractagent.supplement;

import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractRepository;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionRepository;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskRepository;
import com.example.contractagent.task.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TaskSupplementResumeTest {
    @Test
    void submittedTaskSupplementReturnsTaskToReview() {
        SupplementRequestRepository supplementRepository = mock(SupplementRequestRepository.class);
        ComparisonTaskRepository taskRepository = mock(ComparisonTaskRepository.class);
        ContractRepository contractRepository = mock(ContractRepository.class);
        ExtractionRepository extractionRepository = mock(ExtractionRepository.class);
        SupplementService service = new SupplementService(supplementRepository, new ObjectMapper(),
                taskRepository, contractRepository, extractionRepository);

        SupplementRequest request = new SupplementRequest();
        request.setId(5L);
        request.setBusinessId("task:10");
        request.setSupplementType("REJECTED");
        request.setStatus(SupplementStatus.PENDING);
        when(supplementRepository.selectById(5L)).thenReturn(request);

        ComparisonTask task = new ComparisonTask();
        task.setId(10L);
        task.setStatus(TaskStatus.NEEDS_SUPPLEMENT);
        when(taskRepository.selectById(10L)).thenReturn(task);
        Contract buy = new Contract();
        buy.setId(20L);
        when(contractRepository.findByTaskIdAndSide(10L, ContractSide.BUY)).thenReturn(Optional.of(buy));
        Extraction extraction = new Extraction();
        extraction.setId(30L);
        when(extractionRepository.findByContractId(20L)).thenReturn(Optional.of(extraction));

        service.submit(5L, new SubmitSupplementRequest("已补充盖章页"));

        assertThat(request.getStatus()).isEqualTo(SupplementStatus.SUBMITTED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(extraction.getApplicationStatus()).isEqualTo("已补充待复核");
    }

    @Test
    void successfulRetryResolvesFailureRecoveryRequest() {
        SupplementRequestRepository supplementRepository = mock(SupplementRequestRepository.class);
        SupplementService service = new SupplementService(supplementRepository, new ObjectMapper(),
                mock(ComparisonTaskRepository.class), mock(ContractRepository.class),
                mock(ExtractionRepository.class));
        SupplementRequest request = new SupplementRequest();
        request.setId(6L);
        request.setStatus(SupplementStatus.PENDING);
        when(supplementRepository.selectOne(any())).thenReturn(request);

        service.resolveFailureRecovery(10L);

        assertThat(request.getStatus()).isEqualTo(SupplementStatus.RESOLVED);
        verify(supplementRepository).updateById(request);
    }
}
