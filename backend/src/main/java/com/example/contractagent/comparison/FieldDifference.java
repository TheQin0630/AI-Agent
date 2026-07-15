package com.example.contractagent.comparison;

import com.example.contractagent.task.RiskLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldDifference {
    private String field;
    private Object buy;
    private Object sell;
    private String status;   // MATCH / DIFFER / MISSING
    private RiskLevel risk;
}
