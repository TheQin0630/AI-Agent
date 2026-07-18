package com.example.contractagent.agent;

import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractAgentTest {

    private final List<Prompt> prompts = new ArrayList<>();
    private ContractAgent agent;

    @BeforeEach
    void setUp() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return new ChatResponse(List.of(
                    new Generation(new AssistantMessage("assistant-reply-" + prompts.size()))));
        });
        agent = new ContractAgent(ChatClient.builder(model).build(), mock(AgentTools.class));
    }

    @Test
    void forTaskInjectsBoundTaskIdAndForbidsAskingUserForIt() {
        agent.forTask(42L).prompt().user("执行工作流").call().content();

        String systemPrompt = prompts.get(0).getSystemMessage().getText();
        assertTrue(systemPrompt.contains("当前任务 ID：42"));
        assertTrue(systemPrompt.contains("不要向用户询问任务 ID"));
    }

    @Test
    void forTaskInjectsTaskMetadataAndComparisonContext() {
        ComparisonTask task = new ComparisonTask();
        task.setId(1L);
        task.setTitle("测试样例");
        task.setStatus(TaskStatus.CONFIRMED);
        task.setRiskLevel(RiskLevel.HIGH);
        task.setSummary("采购金额高于销售金额");
        task.setComparisonResultJson("{\"riskLevel\":\"HIGH\"}");

        agent.forTask(task).prompt().user("执行工作流").call().content();

        String systemPrompt = prompts.get(0).getSystemMessage().getText();
        assertTrue(systemPrompt.contains("当前任务 ID：1"));
        assertTrue(systemPrompt.contains("任务名称：测试样例"));
        assertTrue(systemPrompt.contains("任务状态：CONFIRMED"));
        assertTrue(systemPrompt.contains("风险等级：HIGH"));
        assertTrue(systemPrompt.contains("已有总结：采购金额高于销售金额"));
        assertTrue(systemPrompt.contains("合同对比结构化结果：{\"riskLevel\":\"HIGH\"}"));
    }

    @Test
    void forTaskReusesMemoryWithinTaskAndIsolatesDifferentTasks() {
        agent.forTask(1L).prompt().user("第一轮问题").call().content();
        agent.forTask(1L).prompt().user("第二轮追问").call().content();
        agent.forTask(2L).prompt().user("另一个任务的问题").call().content();

        List<String> secondTaskOneMessages = messageTexts(prompts.get(1));
        assertTrue(secondTaskOneMessages.contains("第一轮问题"));
        assertTrue(secondTaskOneMessages.contains("assistant-reply-1"));
        assertTrue(secondTaskOneMessages.contains("第二轮追问"));

        List<String> taskTwoMessages = messageTexts(prompts.get(2));
        assertFalse(taskTwoMessages.contains("第一轮问题"));
        assertFalse(taskTwoMessages.contains("assistant-reply-1"));
    }

    private List<String> messageTexts(Prompt prompt) {
        return prompt.getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.USER
                        || message.getMessageType() == MessageType.ASSISTANT)
                .map(Message::getText)
                .toList();
    }
}
