package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
import java.time.LocalDateTime;
public record ConfirmPurchaseResponse(Long id, TaskStatus status,
                                       String applicationNo, String applicationStatus,
                                       LocalDateTime confirmedAt) {}
