package com.example.contractagent.task;

import com.example.contractagent.task.dto.TaskWorkflowDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWorkflowDtoTest {
    @Test
    void mapsEveryTaskStatusToBoundedProgressAndAction() {
        for (TaskStatus status : TaskStatus.values()) {
            TaskWorkflowDto workflow = TaskWorkflowDto.from(status);
            assertThat(workflow.progress()).isBetween(0, 100);
            assertThat(workflow.stage()).isNotBlank();
            assertThat(workflow.stageLabel()).isNotBlank();
            assertThat(workflow.nextAction()).isNotBlank();
        }
    }

    @Test
    void confirmedTaskIsComplete() {
        TaskWorkflowDto workflow = TaskWorkflowDto.from(TaskStatus.CONFIRMED);
        assertThat(workflow.stage()).isEqualTo("COMPLETED");
        assertThat(workflow.progress()).isEqualTo(100);
    }
}
