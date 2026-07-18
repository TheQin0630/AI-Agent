package com.example.contractagent.chat;

import com.example.contractagent.agent.ContractAgent;
import com.example.contractagent.chat.dto.MessageDto;
import com.example.contractagent.chat.dto.MessageListResponse;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ComparisonTaskService taskService;
    private final ChatMessageRepository chatRepo;
    private final ContractAgent agent;

    public com.example.contractagent.chat.dto.ChatResponse chat(Long userId, Long taskId, String content) {
        ComparisonTask task = taskService.requireOwned(taskId, userId);
        if (task.getStatus() == TaskStatus.PENDING) throw BusinessException.of(ErrorCode.PARAM_INVALID, "任务尚未开始");

        // 1. 先保存用户消息（短事务）
        ChatMessage userMsg = new ChatMessage();
        userMsg.setTaskId(taskId); userMsg.setRole(ChatRole.USER); userMsg.setContent(content);
        userMsg.setCreatedAt(LocalDateTime.now());
        chatRepo.insert(userMsg);

        // 2. 调用 LLM（无事务，避免长事务占用数据库连接）
        ChatClient client = agent.forTask(task);
        String reply;
        List<com.example.contractagent.chat.dto.ChatResponse.ToolCall> toolCalls;
        try {
            org.springframework.ai.chat.model.ChatResponse cr = client.prompt().user(content).call().chatResponse();
            AssistantMessage output = cr.getResult().getOutput();
            reply = output.getText();
            List<AssistantMessage.ToolCall> rawCalls = output.getToolCalls();
            toolCalls = rawCalls == null ? Collections.emptyList()
                    : rawCalls.stream()
                        .map(tc -> new com.example.contractagent.chat.dto.ChatResponse.ToolCall(tc.name(), tc.arguments()))
                        .toList();
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "LLM 调用失败: " + e.getMessage());
        }

        // 3. 再保存助手消息（短事务）
        ChatMessage asstMsg = new ChatMessage();
        asstMsg.setTaskId(taskId); asstMsg.setRole(ChatRole.ASSISTANT); asstMsg.setContent(reply);
        asstMsg.setCreatedAt(LocalDateTime.now());
        chatRepo.insert(asstMsg);

        return new com.example.contractagent.chat.dto.ChatResponse(reply, toolCalls);
    }

    public MessageListResponse history(Long userId, Long taskId, int page, int size) {
        taskService.requireOwned(taskId, userId);
        var p = chatRepo.pageByTask(taskId, page, size);
        List<MessageDto> items = p.getRecords().stream()
                .map(m -> new MessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new MessageListResponse(p.getTotal(), items);
    }
}
