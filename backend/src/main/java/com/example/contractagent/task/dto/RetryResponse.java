package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
public record RetryResponse(Long id, TaskStatus status) {}
