package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.*;
import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskRepository;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.task.dto.TaskListResponse;
import com.example.contractagent.task.dto.TaskSummaryDto;
import com.example.contractagent.task.dto.TaskDetailResponse;
import com.example.contractagent.task.TaskDetailService;
import com.example.contractagent.user.User;
import com.example.contractagent.user.UserRepository;
import com.example.contractagent.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService statsService;
    private final UserRepository userRepository;
    private final ComparisonTaskRepository taskRepository;
    private final TaskDetailService taskDetailService;

    @GetMapping("/stats")
    public ApiResponse<StatsResponse> stats() {
        return ApiResponse.ok(statsService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().orderByDesc("created_at");
        if (keyword != null && !keyword.isBlank()) wrapper.like("username", keyword);
        if (enabled != null) wrapper.eq("enabled", enabled);
        var p = userRepository.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        List<AdminUserDto> items = p.getRecords().stream()
                .map(u -> new AdminUserDto(u.getId(), u.getUsername(), u.getRole(), u.getEnabled(), u.getCreatedAt()))
                .toList();
        return ApiResponse.ok(Map.of("total", p.getTotal(), "items", items));
    }

    @PatchMapping("/users/{id}")
    public ApiResponse<AdminUserDto> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        User u = userRepository.selectById(id);
        if (u == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "用户不存在");
        if (req.role() != null) u.setRole(req.role());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        userRepository.updateById(u);
        return ApiResponse.ok(new AdminUserDto(u.getId(), u.getUsername(), u.getRole(), u.getEnabled(), u.getCreatedAt()));
    }

    @GetMapping("/tasks")
    public ApiResponse<TaskListResponse> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) RiskLevel riskLevel) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ComparisonTask>().orderByDesc("created_at");
        if (userId != null) wrapper.eq("user_id", userId);
        if (status != null) wrapper.eq("status", status);
        if (riskLevel != null) wrapper.eq("risk_level", riskLevel);
        var p = taskRepository.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        List<TaskSummaryDto> items = p.getRecords().stream()
                .map(t -> new TaskSummaryDto(t.getId(), t.getTitle(), t.getStatus(), t.getRiskLevel(),
                        t.getConfirmedAt(), t.getConfirmedBy(), t.getCreatedAt(), t.getUpdatedAt()))
                .toList();
        return ApiResponse.ok(new TaskListResponse(p.getTotal(), items));
    }

    /**
     * 任意任务详情（管理端审计，不做所属校验）。
     */
    @GetMapping("/tasks/{id}")
    public ApiResponse<TaskDetailResponse> taskDetail(@PathVariable Long id) {
        return ApiResponse.ok(taskDetailService.getForAdmin(id));
    }
}
