package com.example.contractagent.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comparison_task")
public class ComparisonTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private TaskStatus status;
    private String summary;
    private RiskLevel riskLevel;
    private String comparisonResultJson;
    private String sourceType;
    private String sourceEventId;
    private String sourceSenderId;
    private LocalDateTime confirmedAt;
    private Long confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
