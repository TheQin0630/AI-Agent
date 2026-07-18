package com.example.contractagent.extraction;

import com.example.contractagent.supplement.SupplementService;
import com.example.contractagent.task.ComparisonTaskImportService;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.task.dto.ComparisonTaskImportPayload;
import com.example.contractagent.task.dto.ComparisonTaskImportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalControllerComparisonImportTest {
    @Test
    void acceptsDifyFormDataPayloadAsPlainTextJson() {
        ComparisonTaskImportService importService = mock(ComparisonTaskImportService.class);
        InternalController controller = new InternalController(
                mock(ExtractionService.class), mock(SupplementService.class), importService,
                new ObjectMapper().registerModule(new JavaTimeModule()));
        ReflectionTestUtils.setField(controller, "internalApiKey", "test-key");
        MockMultipartFile buy = new MockMultipartFile("buyFile", "buy.txt", "text/plain", "buy".getBytes());
        MockMultipartFile sell = new MockMultipartFile("sellFile", "sell.txt", "text/plain", "sell".getBytes());
        String payload = """
                {
                  "title":"飞书合同对比",
                  "source_event_id":"om_1",
                  "source_sender_id":"ou_1",
                  "buy_extraction":{"item_name":"服务器","quantity":10},
                  "sell_extraction":{"item_name":"服务器","quantity":10},
                  "summary":"比对完成"
                }
                """;
        when(importService.importTask(any(ComparisonTaskImportPayload.class), any(), any()))
                .thenAnswer(invocation -> {
                    ComparisonTaskImportPayload parsed = invocation.getArgument(0);
                    assertThat(parsed.sourceEventId()).isEqualTo("om_1");
                    assertThat(parsed.buyExtraction().itemName()).isEqualTo("服务器");
                    return new ComparisonTaskImportResponse(1001L, TaskStatus.DONE, true, "/tasks/1001");
                });

        var response = controller.createComparisonTask("test-key", payload, buy, sell);

        assertThat(response.data().taskId()).isEqualTo(1001L);
    }
}
