package com.example.contractagent.user.dto;

public record AuthResponse(String token, long expiresIn, UserDto user) {}
