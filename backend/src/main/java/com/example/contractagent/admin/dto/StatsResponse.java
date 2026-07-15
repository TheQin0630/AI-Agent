package com.example.contractagent.admin.dto;

import com.example.contractagent.task.TaskStatus;
import java.util.Map;

public record StatsResponse(
        long userCount, long taskCount,
        long todayLlmCalls, long todayTokens,
        Map<TaskStatus, Long> tasksByStatus) {}
