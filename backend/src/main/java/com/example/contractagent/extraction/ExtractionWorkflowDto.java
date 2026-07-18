package com.example.contractagent.extraction;

/**
 * Presentation-independent purchase request workflow state.
 */
public record ExtractionWorkflowDto(
        String stage,
        String stageLabel,
        int progress,
        String nextAction
) {
    public static ExtractionWorkflowDto from(String status) {
        if (status == null || status.isBlank()) {
            return new ExtractionWorkflowDto("DRAFT", "待提交", 35, "补全信息后提交");
        }
        return switch (status) {
            case "待确认" -> new ExtractionWorkflowDto("REVIEW", "等待确认", 75, "核对信息并确认或驳回");
            case "已确认" -> new ExtractionWorkflowDto("COMPLETED", "已确认", 100, "查看申请详情");
            case "审批驳回" -> new ExtractionWorkflowDto("SUPPLEMENT", "等待补充", 45, "补充材料后重新提交");
            case "待提交" -> new ExtractionWorkflowDto("DRAFT", "待提交", 50, "提交采购申请");
            default -> new ExtractionWorkflowDto("PROCESSING", status, 55, "查看当前处理结果");
        };
    }
}
