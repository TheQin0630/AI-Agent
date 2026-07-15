package com.example.contractagent.comparison;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.contractagent.task.RiskLevel;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_rule")
public class RiskRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fieldKey;
    private RiskOperator operator;
    private BigDecimal thresholdValue;
    private RiskLevel riskLevel;
    private Boolean enabled;
    private String remark;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
