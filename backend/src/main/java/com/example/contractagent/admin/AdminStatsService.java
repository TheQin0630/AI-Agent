package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.StatsResponse;
import com.example.contractagent.llm.LlmCallLogRepository;
import com.example.contractagent.task.ComparisonTaskRepository;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.user.UserRepository;
import com.example.contractagent.comparison.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final ComparisonTaskRepository taskRepository;
    private final LlmCallLogRepository llmLogRepository;
    private final RiskRuleRepository riskRuleRepository;

    public StatsResponse stats() {
        long userCount = userRepository.selectCount(null);
        long taskCount = taskRepository.selectCount(null);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCalls = llmLogRepository.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.contractagent.llm.LlmCallLog>()
                        .ge("created_at", startOfDay));
        long todayTokens = llmLogRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.contractagent.llm.LlmCallLog>()
                        .ge("created_at", startOfDay))
                .stream().mapToLong(l -> l.getTotalTokens() == null ? 0 : l.getTotalTokens()).sum();
        Map<TaskStatus, Long> byStatus = new HashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, taskRepository.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.contractagent.task.ComparisonTask>()
                            .eq("status", s)));
        }
        return new StatsResponse(userCount, taskCount, todayCalls, todayTokens, byStatus);
    }
}
