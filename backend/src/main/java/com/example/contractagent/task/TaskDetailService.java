package com.example.contractagent.task;

import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.comparison.ComparisonResultStore;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.dto.ContractDetailDto;
import com.example.contractagent.task.dto.TaskDetailResponse;
import com.example.contractagent.task.dto.TaskWorkflowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskDetailService {

    private final ComparisonTaskService taskService;
    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;
    private final ComparisonResultStore comparisonResultStore;

    public TaskDetailResponse get(Long userId, Long taskId) {
        ComparisonTask t = taskService.requireOwned(taskId, userId);
        return buildDetail(t);
    }

    /**
     * 管理端查看任意任务详情（不做所属校验）。
     */
    public TaskDetailResponse getForAdmin(Long taskId) {
        ComparisonTask t = taskService.require(taskId);
        return buildDetail(t);
    }

    private TaskDetailResponse buildDetail(ComparisonTask t) {
        Long taskId = t.getId();
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());
        ComparisonResult cr = comparisonResultStore.read(t.getComparisonResultJson());
        if (cr == null) cr = comparisonService.compare(eBuy, eSell);
        return TaskDetailResponse.builder()
                .id(t.getId()).title(t.getTitle()).status(t.getStatus()).riskLevel(t.getRiskLevel())
                .summary(t.getSummary()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .buy(new ContractDetailDto(buy.getId(), buy.getOriginalFilename(), eBuy))
                .sell(new ContractDetailDto(sell.getId(), sell.getOriginalFilename(), eSell))
                .differences(cr.getDifferences())
                .workflow(TaskWorkflowDto.from(t.getStatus()))
                .build();
    }
}
