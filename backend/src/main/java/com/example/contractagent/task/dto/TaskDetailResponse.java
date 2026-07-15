package com.example.contractagent.task.dto;

import com.example.contractagent.comparison.FieldDifference;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskDetailResponse {
    private Long id;
    private String title;
    private TaskStatus status;
    private RiskLevel riskLevel;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ContractDetailDto buy;
    private ContractDetailDto sell;
    private List<FieldDifference> differences;
}
