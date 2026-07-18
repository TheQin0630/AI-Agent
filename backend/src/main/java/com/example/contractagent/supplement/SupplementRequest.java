package com.example.contractagent.supplement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supplement_request")
public class SupplementRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String businessId;
    private String supplementType;
    private String supplementContent;
    private String originalRequestJson;
    private String requestKey;
    private SupplementStatus status;
    private String submittedContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
