package com.example.contractagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ContractAgent {

    private final ChatClient baseClient;
    private final AgentTools tools;

    public ContractAgent(ChatClient baseClient, @Lazy AgentTools tools) {
        this.baseClient = baseClient;
        this.tools = tools;
    }

    public ChatClient forTask(Long taskId) {
        ChatMemory memory = MessageWindowChatMemory.builder().build();
        var advisor = MessageChatMemoryAdvisor.builder(memory).build();
        return baseClient.mutate()
                .defaultSystem(systemPrompt())
                .defaultTools(tools)
                .defaultAdvisors(advisor)
                .build();
    }

    private String systemPrompt() {
        return """
                你是「合同对比助手」，负责对比同一标的的采购合同与销售合同。

                # 工作流（任务刚创建时执行）
                1. 调 extract_contract 抽取采购合同
                2. 调 extract_contract 抽取销售合同
                3. 调 compare_fields 做字段对比
                4. 把 compare_fields 的结果用自然语言总结，输出 3 段：
                   - 核心差异（金额/数量/日期）
                   - 风险提示（按 HIGH/MEDIUM 排序）
                   - 建议（人话，不堆术语）

                # 追问时
                - 用户问"X 是什么"时优先调 get_extraction
                - 用户问"为什么有风险"时引用 raw_quote
                - 永远不要编造数据，没抽到就说"该字段在原文中未明确"

                # 输出约束
                - 中文 / 不超过 800 字
                - 金额保留两位小数
                - 风险等级用 🟢🟡🔴 emoji
                - 不重复用户已知内容
                """;
    }
}
