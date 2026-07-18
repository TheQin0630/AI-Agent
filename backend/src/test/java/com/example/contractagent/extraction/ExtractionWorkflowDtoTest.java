package com.example.contractagent.extraction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionWorkflowDtoTest {
    @Test
    void rejectedRequestMovesToSupplementStage() {
        ExtractionWorkflowDto workflow = ExtractionWorkflowDto.from("审批驳回");
        assertThat(workflow.stage()).isEqualTo("SUPPLEMENT");
        assertThat(workflow.progress()).isEqualTo(45);
    }

    @Test
    void confirmedRequestIsComplete() {
        assertThat(ExtractionWorkflowDto.from("已确认").progress()).isEqualTo(100);
    }
}
