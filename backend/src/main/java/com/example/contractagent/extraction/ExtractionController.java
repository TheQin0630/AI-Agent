package com.example.contractagent.extraction;

import com.example.contractagent.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 申请单查询与确认接口（供 Dify 写入后的人工确认流程使用）。
 *
 * 流程：
 * 1. Dify Agent 直连 MySQL，INSERT 一条 extraction 记录（application_status='待确认'）
 * 2. Dify 返回确认 URL：http://localhost:5173/extractions/{id}/confirm
 * 3. 用户打开 URL → 前端调用 GET /api/extractions/{id} 展示详情
 * 4. 用户点击"确认" → 前端调用 PATCH /api/extractions/{id}/confirm
 * 5. 用户可在「申请单列表」页（GET /api/extractions）查看所有申请单
 */
@RestController
@RequestMapping("/api/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private final ExtractionService extractionService;

    /**
     * 分页查询申请单列表（按创建时间倒序）。
     *
     * @param page   页码，从 1 开始（默认 1）
     * @param size   每页大小（默认 20）
     * @param status 状态过滤（可选，如 "待确认" / "已确认"）
     */
    @GetMapping
    public ApiResponse<ExtractionListResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(extractionService.list(page, size, status));
    }

    /**
     * 获取申请单详情（确认页面展示用）。
     */
    @GetMapping("/{id}")
    public ApiResponse<ExtractionDetailDto> get(@PathVariable Long id) {
        return ApiResponse.ok(ExtractionDetailDto.from(extractionService.getById(id)));
    }

    /**
     * 确认申请单：状态从"待确认"改为"已确认"。
     */
    @PatchMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        extractionService.confirm(id);
        return ApiResponse.ok(null);
    }
}
