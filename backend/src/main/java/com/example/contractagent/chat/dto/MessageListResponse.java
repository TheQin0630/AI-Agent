package com.example.contractagent.chat.dto;
import java.util.List;
public record MessageListResponse(long total, List<MessageDto> items) {}
