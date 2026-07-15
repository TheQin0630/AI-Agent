package com.example.contractagent.admin.dto;

import com.example.contractagent.comparison.RiskOperator;
import com.example.contractagent.task.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskRuleDto(
        Long id, String fieldKey, RiskOperator operator, BigDecimal thresholdValue,
        RiskLevel riskLevel, Boolean enabled, String remark, LocalDateTime updatedAt) {}
