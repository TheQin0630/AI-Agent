package com.example.contractagent.task;

import com.example.contractagent.admin.ReportService;
import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.task.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ComparisonTaskService taskService;
    private final TaskDetailService taskDetailService;
    private final ReportService reportService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<CreateTaskResponse> create(
            @AuthenticationPrincipal Long userId,
            @RequestParam("title") String title,
            @RequestParam("buyFile") MultipartFile buyFile,
            @RequestParam("sellFile") MultipartFile sellFile) {
        return ApiResponse.ok(taskService.create(userId, title, buyFile, sellFile));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskDetailResponse> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(taskDetailService.get(userId, id));
    }

    @GetMapping
    public ApiResponse<TaskListResponse> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskService.list(userId, page, size, status));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<RetryResponse> retry(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(taskService.retry(userId, id));
    }

    /**
     * 人工确认创建采购单（仅 DONE 状态可调用）。
     * 确认后任务转 CONFIRMED，BUY 侧 extraction 的 application_status 升为「已确认」。
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<ConfirmPurchaseResponse> confirmPurchase(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ApiResponse.ok(taskService.confirmPurchase(userId, id));
    }

    @GetMapping(value = "/{id}/report", produces = "text/markdown; charset=utf-8")
    public String report(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        taskService.requireOwned(id, userId);
        return reportService.generateMarkdown(id);
    }
}
