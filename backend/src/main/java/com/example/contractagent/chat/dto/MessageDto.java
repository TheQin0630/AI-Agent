package com.example.contractagent.chat.dto;
import com.example.contractagent.chat.ChatRole;
import java.time.LocalDateTime;
public record MessageDto(Long id, ChatRole role, String content, LocalDateTime createdAt) {}
