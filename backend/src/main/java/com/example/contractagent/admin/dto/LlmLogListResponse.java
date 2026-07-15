package com.example.contractagent.admin.dto;
import java.util.List;
public record LlmLogListResponse(long total, List<LlmLogDto> items) {}
