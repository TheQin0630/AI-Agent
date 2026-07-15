package com.example.contractagent.admin.dto;

import com.example.contractagent.user.UserRole;

public record UpdateUserRequest(UserRole role, Boolean enabled) {}
