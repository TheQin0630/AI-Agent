package com.example.contractagent.admin.dto;

import com.example.contractagent.user.UserRole;
import java.time.LocalDateTime;

public record AdminUserDto(Long id, String username, UserRole role, Boolean enabled, LocalDateTime createdAt) {}
