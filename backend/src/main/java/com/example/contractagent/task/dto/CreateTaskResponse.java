package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
import java.time.LocalDateTime;
public record CreateTaskResponse(Long id, String title, TaskStatus status, LocalDateTime createdAt) {}
