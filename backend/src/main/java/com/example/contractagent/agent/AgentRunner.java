package com.example.contractagent.agent;

import com.example.contractagent.chat.ChatMessage;
import com.example.contractagent.chat.ChatMessageRepository;
import com.example.contractagent.chat.ChatRole;
import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonResultStore;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
public class AgentRunner {

    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;
    private final ComparisonResultStore comparisonResultStore;
    private final TaskExecutor taskExecutor;
    private final ComparisonTaskService taskService;
    private final ChatMessageRepository chatRepo;
    private final ContractAgent agent;

    public AgentRunner(ContractService contractService,
                       ExtractionService extractionService,
                       ComparisonService comparisonService,
                       ComparisonResultStore comparisonResultStore,
                       TaskExecutor taskExecutor,
                       @Lazy ComparisonTaskService taskService,
                       ChatMessageRepository chatRepo,
                       ContractAgent agent) {
        this.contractService = contractService;
        this.extractionService = extractionService;
        this.comparisonService = comparisonService;
        this.comparisonResultStore = comparisonResultStore;
        this.taskExecutor = taskExecutor;
        this.taskService = taskService;
        this.chatRepo = chatRepo;
        this.agent = agent;
    }

    @Async
    public void run(Long taskId) {
        try {
            ComparisonTask task = taskService.require(taskId);

            // 直接调 Service 完成抽取和对比（避免 LLM 反复调用）
            Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
            Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
            CompletableFuture<Extraction> buyExtraction = CompletableFuture.supplyAsync(
                    () -> extractionService.extractAndSave(taskId, task.getUserId(), buy), taskExecutor);
            CompletableFuture<Extraction> sellExtraction = CompletableFuture.supplyAsync(
                    () -> extractionService.extractAndSave(taskId, task.getUserId(), sell), taskExecutor);
            CompletableFuture.allOf(buyExtraction, sellExtraction).join();
            Extraction eBuy = buyExtraction.join();
            Extraction eSell = sellExtraction.join();
            ComparisonResult result = comparisonService.compare(eBuy, eSell);

            // 让 Agent 基于结果生成自然语言总结
            ChatClient client = agent.forTask(taskId);
            String summary = client.prompt()
                    .user("compare_fields 已完成，结果如下：%s。请用自然语言总结成 3 段：核心差异 / 风险提示 / 建议。".formatted(result.toString()))
                    .call()
                    .content();

            taskService.updateResult(taskId, summary, result.getRiskLevel(), comparisonResultStore.write(result));

            // 落库首条助手消息
            ChatMessage msg = new ChatMessage();
            msg.setTaskId(taskId);
            msg.setRole(ChatRole.ASSISTANT);
            msg.setContent(summary);
            msg.setCreatedAt(LocalDateTime.now());
            chatRepo.insert(msg);
        } catch (Exception e) {
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        }
    }
}
