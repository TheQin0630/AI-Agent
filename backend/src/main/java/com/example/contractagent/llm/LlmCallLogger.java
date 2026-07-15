package com.example.contractagent.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LlmCallLogger {
    private final LlmCallLogRepository repo;

    public void log(Long taskId, Long userId, String model,
                    Integer prompt, Integer completion, Integer total,
                    long durationMs, String status, String errorMsg) {
        LlmCallLog log = new LlmCallLog();
        log.setTaskId(taskId); log.setUserId(userId); log.setModel(model);
        log.setPromptTokens(prompt); log.setCompletionTokens(completion); log.setTotalTokens(total);
        log.setDurationMs((int) durationMs); log.setStatus(status); log.setErrorMsg(errorMsg);
        log.setCreatedAt(LocalDateTime.now());
        repo.insert(log);
    }
}
