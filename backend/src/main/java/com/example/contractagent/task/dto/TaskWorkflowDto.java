package com.example.contractagent.task.dto;

import com.example.contractagent.task.TaskStatus;

/**
 * Stable workflow semantics shared by task list and detail clients.
 */
public record TaskWorkflowDto(
        String stage,
        String stageLabel,
        int progress,
        String nextAction
) {
    public static TaskWorkflowDto from(TaskStatus status) {
        if (status == null) return new TaskWorkflowDto("UNKNOWN", "状态待同步", 0, "刷新后重试");
        return switch (status) {
            case PENDING -> new TaskWorkflowDto("QUEUED", "等待处理", 15, "等待智能分析启动");
            case RUNNING -> new TaskWorkflowDto("COMPARING", "智能对比中", 58, "等待字段比对完成");
            case DONE -> new TaskWorkflowDto("REVIEW", "等待复核", 90, "复核结果并确认采购申请");
            case NEEDS_SUPPLEMENT -> new TaskWorkflowDto("SUPPLEMENT", "等待补充", 75, "根据驳回原因补充材料后重新提交");
            case CONFIRMED -> new TaskWorkflowDto("COMPLETED", "已完成", 100, "查看采购申请单");
            case FAILED -> new TaskWorkflowDto("FAILED", "处理失败", 35, "检查异常并重新分析");
        };
    }
}
