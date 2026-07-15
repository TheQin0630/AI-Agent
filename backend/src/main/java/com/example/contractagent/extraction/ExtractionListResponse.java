package com.example.contractagent.extraction;

import java.util.List;

/**
 * 申请单列表分页响应
 */
public record ExtractionListResponse(long total, List<ExtractionListItemDto> items) {}
