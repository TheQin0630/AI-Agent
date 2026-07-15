package com.example.contractagent.comparison;

import com.example.contractagent.task.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ComparisonResult {
    private List<FieldDifference> differences;
    private RiskLevel riskLevel;
}
