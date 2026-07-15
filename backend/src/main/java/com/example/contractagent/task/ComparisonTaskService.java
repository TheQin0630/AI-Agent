package com.example.contractagent.task;

import com.example.contractagent.agent.AgentRunner;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
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
                        t.getConfirmedAt(), t.getConfirmedBy(), t.getCreatedAt(), t.getUpdatedAt()))
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
     * 人工确认创建采购单：仅 DONE 状态可确认。
     * 确认后任务转 CONFIRMED，BUY 侧 extraction 的 application_status 升为「已确认」，
     * message 改为「已人工确认创建采购单」。
     */
    @Transactional
    public ConfirmPurchaseResponse confirmPurchase(Long userId, Long taskId) {
        ComparisonTask t = requireOwned(taskId, userId);
        if (t.getStatus() != TaskStatus.DONE) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅 DONE 状态可确认创建采购单");
        }
        // 1. 更新任务状态 + 审计字段
        LocalDateTime now = LocalDateTime.now();
        t.setStatus(TaskStatus.CONFIRMED);
        t.setConfirmedAt(now);
        t.setConfirmedBy(userId);
        taskRepository.updateById(t);

        // 2. 同步更新 BUY 侧 extraction 的 application_status / message
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        extractionService.updateApplicationStatus(eBuy.getId(), "已确认", "已人工确认创建采购单");

        return new ConfirmPurchaseResponse(t.getId(), t.getStatus(),
                eBuy.getApplicationNo(), "已确认", now);
    }
}
