package com.example.contractagent.task;

import com.example.contractagent.agent.AgentRunner;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
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
import com.example.contractagent.supplement.CreateSupplementRequest;
import com.example.contractagent.supplement.SupplementResponse;
import com.example.contractagent.supplement.SupplementService;
import com.example.contractagent.task.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComparisonTaskService {

    private final ComparisonTaskRepository taskRepository;
    private final ContractService contractService;
    private final AgentRunner agentRunner;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;
    private final ComparisonResultStore comparisonResultStore;
    private final PurchaseOrderService purchaseOrderService;
    private final SupplementService supplementService;

    @Transactional
    public CreateTaskResponse create(Long userId, String title, MultipartFile buyFile, MultipartFile sellFile) {
        ComparisonTask t = new ComparisonTask();
        t.setUserId(userId);
        t.setTitle(title);
        t.setStatus(TaskStatus.PENDING);
        // 显式设置 createdAt，避免 insert 后未回填导致响应中为 null
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        taskRepository.insert(t);

        contractService.saveForTask(t.getId(), ContractSide.BUY, buyFile);
        contractService.saveForTask(t.getId(), ContractSide.SELL, sellFile);

        t.setStatus(TaskStatus.RUNNING);
        taskRepository.updateById(t);

        // 触发 Agent 异步跑：必须在事务提交后触发，否则异步线程读取不到未提交的数据
        final Long taskId = t.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try { agentRunner.run(taskId); } catch (Exception ignored) {}
                }
            });
        } else {
            try { agentRunner.run(taskId); } catch (Exception ignored) {}
        }

        return new CreateTaskResponse(t.getId(), t.getTitle(), t.getStatus(), t.getCreatedAt());
    }

    public TaskListResponse list(Long userId, int page, int size, TaskStatus status) {
        var p = taskRepository.pageByUser(userId, page, size, status);
        List<TaskSummaryDto> items = p.getRecords().stream()
                .map(t -> new TaskSummaryDto(t.getId(), t.getTitle(), t.getStatus(), t.getRiskLevel(),
                        t.getConfirmedAt(), t.getConfirmedBy(), t.getCreatedAt(), t.getUpdatedAt(),
                        TaskWorkflowDto.from(t.getStatus())))
                .toList();
        return new TaskListResponse(p.getTotal(), items);
    }

    public ComparisonTask requireOwned(Long taskId, Long userId) {
        ComparisonTask t = taskRepository.selectById(taskId);
        if (t == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        if (!t.getUserId().equals(userId)) throw BusinessException.of(ErrorCode.FORBIDDEN, "无权访问该任务");
        return t;
    }

    public ComparisonTask require(Long taskId) {
        ComparisonTask t = taskRepository.selectById(taskId);
        if (t == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        return t;
    }

    public void updateStatus(Long taskId, TaskStatus status) {
        ComparisonTask t = require(taskId);
        t.setStatus(status);
        taskRepository.updateById(t);
    }

    public void updateResult(Long taskId, String summary, RiskLevel riskLevel, String comparisonResultJson) {
        ComparisonTask t = require(taskId);
        t.setSummary(summary);
        t.setRiskLevel(riskLevel);
        t.setComparisonResultJson(comparisonResultJson);
        t.setStatus(TaskStatus.DONE);
        taskRepository.updateById(t);
    }

    public RetryResponse retry(Long userId, Long taskId) {
        ComparisonTask t = requireOwned(taskId, userId);
        if (t.getStatus() != TaskStatus.FAILED) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅 FAILED 状态可重跑");
        }
        t.setStatus(TaskStatus.RUNNING);
        taskRepository.updateById(t);
        // 事务提交后触发异步
        final Long id = t.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try { agentRunner.run(id); } catch (Exception ignored) {}
                }
            });
        } else {
            try { agentRunner.run(id); } catch (Exception ignored) {}
        }
        return new RetryResponse(t.getId(), t.getStatus());
    }

    /**
     * 审批合同对比结果：仅 DONE 状态可审批。
     * 审批后任务转 CONFIRMED，并创建或激活一张待确认的采购申请；此阶段不创建采购订单。
     */
    @Transactional
    public ConfirmPurchaseResponse confirmPurchase(Long userId, Long taskId) {
        ComparisonTask t = requireOwned(taskId, userId);
        if (t.getStatus() == TaskStatus.CONFIRMED) {
            Contract confirmedBuy = contractService.requireForTask(taskId, ContractSide.BUY);
            Extraction confirmedExtraction = extractionService.getByContractId(confirmedBuy.getId());
            return applicationReadyResponse(t, confirmedExtraction, "合同对比已审批，采购申请等待确认");
        }
        if (t.getStatus() != TaskStatus.DONE) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅分析完成的合同对比任务可审批通过");
        }
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());

        ComparisonResult comparison = comparisonService.compare(eBuy, eSell);
        t.setRiskLevel(comparison.getRiskLevel());
        t.setComparisonResultJson(comparisonResultStore.write(comparison));
        LocalDateTime now = LocalDateTime.now();
        t.setStatus(TaskStatus.CONFIRMED);
        t.setConfirmedAt(now);
        t.setConfirmedBy(userId);
        taskRepository.updateById(t);

        Extraction application = extractionService.prepareForConfirmation(eBuy.getId());
        return applicationReadyResponse(t, application, "审批通过，采购申请已创建并等待确认");
    }

    @Transactional
    public ConfirmPurchaseResponse confirmApplication(Long userId, Long extractionId) {
        Extraction application = extractionService.getById(extractionId);
        ComparisonTask task = null;
        Long taskId = null;
        if (application.getContractId() != null) {
            Contract buy = contractService.getById(application.getContractId());
            if (buy == null || buy.getSide() != ContractSide.BUY) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅采购合同对应的申请单可确认");
            }
            task = requireOwned(buy.getTaskId(), userId);
            taskId = task.getId();
            if (task.getStatus() != TaskStatus.CONFIRMED) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "请先审批通过对应的合同对比任务");
            }
        }

        if ("已确认".equals(application.getApplicationStatus())) {
            PurchaseOrderResult existing = purchaseOrderService.getConfirmedResult(taskId, application);
            return response(task, application, existing, "采购申请已处理，未重复创建采购订单");
        }
        if (!"待确认".equals(application.getApplicationStatus())) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "仅待确认状态的采购申请可确认，当前状态: " + application.getApplicationStatus());
        }

        PurchaseOrderResult orderResult = purchaseOrderService.createOrGet(taskId, userId, application);
        String message = orderResult.inventorySufficient()
                ? "库存充足，采购申请已确认，无需创建采购订单"
                : "采购申请已确认，正式采购订单已创建并提交";
        extractionService.updateApplicationStatus(application.getId(), "已确认", message);
        return response(task, application, orderResult, message);
    }

    private ConfirmPurchaseResponse applicationReadyResponse(ComparisonTask task, Extraction application,
                                                              String message) {
        return new ConfirmPurchaseResponse(task.getId(), task.getStatus(),
                application.getApplicationNo(), application.getApplicationStatus(), task.getConfirmedAt(),
                null, null, null, null, null, "NOT_CHECKED", message);
    }

    private ConfirmPurchaseResponse response(ComparisonTask task, Extraction buy,
                                             PurchaseOrderResult result, String message) {
        PurchaseOrder order = result.order();
        return new ConfirmPurchaseResponse(task == null ? null : task.getId(),
                task == null ? null : task.getStatus(),
                buy.getApplicationNo(), "已确认",
                task == null ? LocalDateTime.now() : task.getConfirmedAt(),
                order == null ? null : order.getId(),
                order == null ? null : order.getOrderNo(),
                order == null ? null : order.getStatus(),
                order == null ? null : order.getOrderQuantity(),
                order == null ? null : order.getTotalAmount(),
                order == null ? "SUFFICIENT" : order.getInventoryCheckStatus(), message);
    }

    @Transactional
    public RejectTaskResponse rejectForSupplement(Long userId, Long taskId, String reason) {
        ComparisonTask task = requireOwned(taskId, userId);
        if (task.getStatus() == TaskStatus.NEEDS_SUPPLEMENT) {
            var existing = supplementService.findLatestForBusiness("task:" + taskId, "REJECTED");
            if (existing == null) {
                throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "任务等待补充，但补充请求不存在");
            }
            return new RejectTaskResponse(taskId, task.getStatus(), SupplementResponse.from(existing),
                    "该任务已驳回，未重复创建补充请求");
        }
        if (task.getStatus() != TaskStatus.DONE) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅等待复核的任务可以驳回补充");
        }

        String cleanReason = reason.trim();
        String requestKey = "task:" + taskId + ":REJECTED:" + task.getUpdatedAt();
        var supplement = supplementService.createOrGet(new CreateSupplementRequest(
                "task:" + taskId, "REJECTED", cleanReason, null, requestKey));

        task.setStatus(TaskStatus.NEEDS_SUPPLEMENT);
        taskRepository.updateById(task);
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Extraction buyExtraction = extractionService.getByContractId(buy.getId());
        extractionService.updateApplicationStatus(buyExtraction.getId(), "待补充", "审批驳回：" + cleanReason);
        return new RejectTaskResponse(taskId, task.getStatus(), SupplementResponse.from(supplement),
                "任务已驳回并创建补充请求");
    }
}
