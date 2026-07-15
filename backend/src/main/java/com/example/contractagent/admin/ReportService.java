package com.example.contractagent.admin;

import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.comparison.ComparisonResultStore;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.TaskDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ComparisonTaskService taskService;
    private final TaskDetailService taskDetailService;
    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;
    private final ComparisonResultStore comparisonResultStore;

    public String generateMarkdown(Long taskId) {
        ComparisonTask t = taskService.require(taskId);
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());
        var cr = comparisonResultStore.read(t.getComparisonResultJson());
        if (cr == null) cr = comparisonService.compare(eBuy, eSell);

        var df = DateTimeFormatter.ISO_LOCAL_DATE;
        var sb = new StringBuilder();
        sb.append("# 合同对比报告：").append(t.getTitle()).append("\n\n");
        sb.append("- 任务ID：").append(t.getId()).append("\n");
        sb.append("- 生成时间：").append(t.getUpdatedAt()).append("\n");
        sb.append("- 整体风险：").append(t.getRiskLevel()).append("\n");
        sb.append("- 任务状态：").append(t.getStatus()).append("\n");
        if (t.getConfirmedAt() != null) {
            sb.append("- 采购单确认时间：").append(t.getConfirmedAt()).append("\n");
            sb.append("- 确认人ID：").append(t.getConfirmedBy()).append("\n");
        } else {
            sb.append("- 采购单确认状态：**未确认**\n");
        }
        sb.append("\n");

        sb.append("## 1. 要素对比表\n\n");
        sb.append("| 字段 | 采购合同 | 销售合同 | 差异 |\n");
        sb.append("|---|---|---|---|\n");
        for (var d : cr.getDifferences()) {
            sb.append("| ").append(d.getField())
              .append(" | ").append(d.getBuy() == null ? "—" : d.getBuy())
              .append(" | ").append(d.getSell() == null ? "—" : d.getSell())
              .append(" | ").append(d.getStatus()).append(d.getRisk() != null ? " (" + d.getRisk() + ")" : "")
              .append(" |\n");
        }
        sb.append("\n## 2. Agent 风险总结\n\n");
        sb.append(t.getSummary() == null ? "（无）" : t.getSummary()).append("\n");

        return sb.toString();
    }
}
