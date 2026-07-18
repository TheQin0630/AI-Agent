package com.example.contractagent.supplement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.example.contractagent.contract.ContractRepository;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.ExtractionRepository;
import com.example.contractagent.task.ComparisonTaskRepository;
import com.example.contractagent.task.TaskStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplementService {
    private final SupplementRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final ComparisonTaskRepository taskRepository;
    private final ContractRepository contractRepository;
    private final ExtractionRepository extractionRepository;

    @Transactional
    public SupplementRequest createOrGet(CreateSupplementRequest input) {
        SupplementRequest existing = findByRequestKey(input.requestKey());
        if (existing != null) return existing;

        SupplementRequest request = new SupplementRequest();
        request.setBusinessId(input.businessId().trim());
        request.setSupplementType(input.supplementType());
        request.setSupplementContent(input.supplementContent().trim());
        request.setRequestKey(input.requestKey().trim());
        request.setStatus(SupplementStatus.PENDING);
        try {
            request.setOriginalRequestJson(input.originalRequest() == null ? null : objectMapper.writeValueAsString(input.originalRequest()));
        } catch (JsonProcessingException ex) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "originalRequest 不是有效 JSON");
        }
        try {
            repository.insert(request);
            return request;
        } catch (DuplicateKeyException ex) {
            SupplementRequest raced = findByRequestKey(input.requestKey());
            if (raced != null) return raced;
            throw ex;
        }
    }

    public SupplementRequest get(Long id) {
        SupplementRequest request = repository.selectById(id);
        if (request == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "补充请求不存在");
        return request;
    }

    @Transactional
    public SupplementRequest submit(Long id, SubmitSupplementRequest input) {
        SupplementRequest request = get(id);
        if (request.getStatus() == SupplementStatus.RESOLVED) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "该补充请求已处理完成");
        }
        request.setSubmittedContent(input.content().trim());
        request.setStatus(SupplementStatus.SUBMITTED);
        repository.updateById(request);
        resumeTaskReviewIfNeeded(request);
        return request;
    }

    public SupplementRequest findLatestForBusiness(String businessId, String supplementType) {
        return repository.selectOne(new LambdaQueryWrapper<SupplementRequest>()
                .eq(SupplementRequest::getBusinessId, businessId)
                .eq(SupplementRequest::getSupplementType, supplementType)
                .orderByDesc(SupplementRequest::getId)
                .last("LIMIT 1"));
    }

    @Transactional
    public void resolveFailureRecovery(Long taskId) {
        SupplementRequest request = findByRequestKey("task:" + taskId + ":FAILURE_RECOVERY");
        if (request == null || request.getStatus() == SupplementStatus.RESOLVED) return;
        request.setStatus(SupplementStatus.RESOLVED);
        repository.updateById(request);
    }

    private void resumeTaskReviewIfNeeded(SupplementRequest request) {
        if (!"REJECTED".equals(request.getSupplementType()) ||
                request.getBusinessId() == null || !request.getBusinessId().startsWith("task:")) return;
        Long taskId;
        try { taskId = Long.valueOf(request.getBusinessId().substring("task:".length())); }
        catch (NumberFormatException ignored) { return; }

        var task = taskRepository.selectById(taskId);
        if (task == null || task.getStatus() != TaskStatus.NEEDS_SUPPLEMENT) return;
        task.setStatus(TaskStatus.DONE);
        taskRepository.updateById(task);
        contractRepository.findByTaskIdAndSide(taskId, ContractSide.BUY)
                .flatMap(contract -> extractionRepository.findByContractId(contract.getId()))
                .ifPresent(extraction -> {
                    extraction.setApplicationStatus("已补充待复核");
                    extraction.setMessage("补充材料已提交，等待重新复核");
                    extractionRepository.updateById(extraction);
                });
    }

    private SupplementRequest findByRequestKey(String requestKey) {
        return repository.selectOne(new LambdaQueryWrapper<SupplementRequest>()
                .eq(SupplementRequest::getRequestKey, requestKey.trim()).last("LIMIT 1"));
    }
}
