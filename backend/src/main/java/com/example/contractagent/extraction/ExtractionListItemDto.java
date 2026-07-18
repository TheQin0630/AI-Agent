package com.example.contractagent.extraction;

import java.time.LocalDateTime;

/**
 * 申请单列表项 DTO（用于申请单列表页展示）
 */
public record ExtractionListItemDto(
        Long id,
        String applicationNo,
        String applicationTitle,
        String applicationStatus,
        String supplierName,
        String itemName,
        String applicationType,
        LocalDateTime createTime,
        LocalDateTime extractedAt,
        ExtractionWorkflowDto workflow
) {
    public static ExtractionListItemDto from(Extraction e) {
        return new ExtractionListItemDto(
                e.getId(),
                e.getApplicationNo(),
                e.getApplicationTitle(),
                e.getApplicationStatus(),
                e.getSupplierName(),
                e.getItemName(),
                e.getApplicationType(),
                e.getCreateTime(),
                e.getExtractedAt(),
                ExtractionWorkflowDto.from(e.getApplicationStatus())
        );
    }
}
