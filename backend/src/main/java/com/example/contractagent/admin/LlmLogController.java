package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.LlmLogDto;
import com.example.contractagent.admin.dto.LlmLogListResponse;
import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.llm.LlmCallLog;
import com.example.contractagent.llm.LlmCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/llm-logs")
@RequiredArgsConstructor
public class LlmLogController {

    private final LlmCallLogRepository repository;

    @GetMapping
    public ApiResponse<LlmLogListResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        var p = repository.pageFiltered(page, size, userId, model, from, to);
        List<LlmLogDto> items = p.getRecords().stream()
                .map(l -> new LlmLogDto(l.getId(), l.getTaskId(), l.getUserId(), l.getModel(),
                        l.getPromptTokens(), l.getCompletionTokens(), l.getTotalTokens(),
                        l.getDurationMs(), l.getStatus(), l.getErrorMsg(), l.getCreatedAt()))
                .toList();
        return ApiResponse.ok(new LlmLogListResponse(p.getTotal(), items));
    }
}
