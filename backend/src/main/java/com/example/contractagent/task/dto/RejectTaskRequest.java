package com.example.contractagent.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTaskRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 1000, message = "驳回原因不能超过 1000 字")
        String reason) {
}
