package com.example.contractagent.task.dto;

import com.example.contractagent.task.TaskStatus;

public record ComparisonTaskImportResponse(Long taskId, TaskStatus status, boolean created, String taskUrl) {}
