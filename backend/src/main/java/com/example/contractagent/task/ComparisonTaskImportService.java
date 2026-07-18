package com.example.contractagent.task;

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
import com.example.contractagent.task.dto.ComparisonTaskImportPayload;
import com.example.contractagent.task.dto.ComparisonTaskImportResponse;
import com.example.contractagent.user.User;
import com.example.contractagent.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ComparisonTaskImportService {
    private static final String SOURCE_TYPE = "FEISHU";

    private final ComparisonTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;
    private final ComparisonResultStore comparisonResultStore;

    @Value("${internal.dify-task-owner-username:admin}")
    private String ownerUsername;

    @Value("${internal.frontend-url:http://38.22.95.144:5173}")
    private String frontendUrl;

    @Transactional
    public ComparisonTaskImportResponse importTask(ComparisonTaskImportPayload payload,
                                                   MultipartFile buyFile,
                                                   MultipartFile sellFile) {
        validatePayload(payload);
        validateFile(buyFile, "buyFile");
        validateFile(sellFile, "sellFile");

        ComparisonTask existing = taskRepository.findBySource(SOURCE_TYPE, payload.sourceEventId()).orElse(null);
        if (existing != null) return response(existing, false);

        User owner = userRepository.findByUsername(ownerUsername)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> BusinessException.of(ErrorCode.SYSTEM_ERROR,
                        "Dify 任务归属账号不存在或已禁用: " + ownerUsername));

        LocalDateTime now = LocalDateTime.now();
        ComparisonTask task = new ComparisonTask();
        task.setUserId(owner.getId());
        task.setTitle(payload.title().trim());
        task.setStatus(TaskStatus.PENDING);
        task.setSourceType(SOURCE_TYPE);
        task.setSourceEventId(payload.sourceEventId().trim());
        task.setSourceSenderId(payload.sourceSenderId().trim());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        if (taskRepository.insertIgnoreSource(task) == 0) {
            ComparisonTask concurrent = taskRepository.findBySource(SOURCE_TYPE, payload.sourceEventId())
                    .orElseThrow(() -> BusinessException.of(ErrorCode.SYSTEM_ERROR,
                            "幂等任务已存在但暂时无法读取，请重试"));
            return response(concurrent, false);
        }

        Contract buy = contractService.saveForTask(task.getId(), ContractSide.BUY, buyFile);
        Contract sell = contractService.saveForTask(task.getId(), ContractSide.SELL, sellFile);
        Extraction buyExtraction = extractionService.createFromDify(buy.getId(), payload.buyExtraction(), null);
        Extraction sellExtraction = extractionService.createFromDify(sell.getId(), payload.sellExtraction(), null);

        ComparisonResult comparison = comparisonService.compare(buyExtraction, sellExtraction);
        task.setSummary(payload.summary());
        task.setRiskLevel(comparison.getRiskLevel());
        task.setComparisonResultJson(comparisonResultStore.write(comparison));
        task.setStatus(TaskStatus.DONE);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.updateById(task);
        return response(task, true);
    }

    private void validateFile(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, field + " 不能为空");
        }
    }

    private void validatePayload(ComparisonTaskImportPayload payload) {
        if (payload == null || payload.title() == null || payload.title().isBlank()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "payload.title 不能为空");
        }
        if (payload.sourceEventId() == null || payload.sourceEventId().isBlank()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "payload.source_event_id 不能为空");
        }
        if (payload.sourceSenderId() == null || payload.sourceSenderId().isBlank()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "payload.source_sender_id 不能为空");
        }
        if (payload.buyExtraction() == null || payload.sellExtraction() == null) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "payload.buy_extraction 和 payload.sell_extraction 不能为空");
        }
    }

    private ComparisonTaskImportResponse response(ComparisonTask task, boolean created) {
        String url = frontendUrl.replaceAll("/+$", "") + "/tasks/" + task.getId();
        return new ComparisonTaskImportResponse(task.getId(), task.getStatus(), created, url);
    }
}
