package com.example.contractagent.task.dto;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import java.time.LocalDateTime;
public record TaskSummaryDto(Long id, String title, TaskStatus status, RiskLevel riskLevel,
                            LocalDateTime confirmedAt, Long confirmedBy,
                            LocalDateTime createdAt, LocalDateTime updatedAt,
                            TaskWorkflowDto workflow) {}
