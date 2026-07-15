package com.example.contractagent.chat.dto;
import java.util.List;
public record ChatResponse(String reply, List<ToolCall> toolCalls) {
    public record ToolCall(String tool, String args) {}
}
