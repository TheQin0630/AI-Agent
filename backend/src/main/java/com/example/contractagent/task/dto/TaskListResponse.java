package com.example.contractagent.task.dto;
import java.util.List;
public record TaskListResponse(long total, List<TaskSummaryDto> items) {}
