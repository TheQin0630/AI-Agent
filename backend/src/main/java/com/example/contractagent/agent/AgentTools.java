package com.example.contractagent.agent;

import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.chat.ChatMessage;
import com.example.contractagent.chat.ChatMessageRepository;
import com.example.contractagent.chat.ChatRole;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentTools {

    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonTaskService taskService;
    private final ComparisonService comparisonService;
    private final ChatMessageRepository chatRepo;

    @Tool(description = "抽取指定合同的关键要素，返回结构化结果")
    public Extraction extractContract(Long contractId) {
        Contract c = contractService.getById(contractId);
        if (c == null) {
            throw new IllegalStateException("合同不存在: " + contractId);
        }
        return extractionService.extractAndSave(c.getTaskId(), null, c);
    }

    @Tool(description = "获取合同已抽取的要素（不重新抽取）")
    public Extraction getExtraction(Long contractId) {
        return extractionService.getByContractId(contractId);
    }

    @Tool(description = "对比任务下两份合同的全部字段，返回差异列表和整体风险等级")
    public ComparisonResult compareFields(Long taskId) {
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());
        return comparisonService.compare(eBuy, eSell);
    }

    @Tool(description = "查看本任务历史对话")
    public List<ChatMessage> getChatHistory(Long taskId) {
        return chatRepo.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                .eq("task_id", taskId).orderByAsc("created_at"));
    }
}
