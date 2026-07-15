package com.example.contractagent.admin.dto;

import java.time.LocalDateTime;

public record LlmLogDto(Long id, Long taskId, Long userId, String model,
                         Integer promptTokens, Integer completionTokens, Integer totalTokens,
                         Integer durationMs, String status, String errorMsg, LocalDateTime createdAt) {}
