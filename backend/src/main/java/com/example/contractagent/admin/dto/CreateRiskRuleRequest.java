package com.example.contractagent.admin.dto;

import com.example.contractagent.comparison.RiskOperator;
import com.example.contractagent.task.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateRiskRuleRequest(
        @NotBlank String fieldKey,
        @NotNull RiskOperator operator,
        BigDecimal thresholdValue,
        @NotNull RiskLevel riskLevel,
        Boolean enabled,
        String remark) {}
