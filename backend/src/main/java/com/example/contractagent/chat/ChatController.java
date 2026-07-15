package com.example.contractagent.chat;

import com.example.contractagent.chat.dto.ChatRequest;
import com.example.contractagent.chat.dto.ChatResponse;
import com.example.contractagent.chat.dto.MessageListResponse;
import com.example.contractagent.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks/{taskId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestBody @Valid ChatRequest req) {
        return ApiResponse.ok(chatService.chat(userId, taskId, req.content()));
    }

    @GetMapping("/messages")
    public ApiResponse<MessageListResponse> messages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(chatService.history(userId, taskId, page, size));
    }
}
