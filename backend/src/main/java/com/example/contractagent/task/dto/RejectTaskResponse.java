package com.example.contractagent.task.dto;

import com.example.contractagent.supplement.SupplementResponse;
import com.example.contractagent.task.TaskStatus;

public record RejectTaskResponse(Long taskId, TaskStatus status,
                                 SupplementResponse supplement, String message) {
}
