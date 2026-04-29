package org.example.graph.handler;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;
import org.example.security.ApprovalService;

public class ApprovalStepHandler implements PlanStepHandler {

    private final ApprovalService approvalService;

    public ApprovalStepHandler(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public String agentType() {
        return "approval";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            String taskId = state.value("task_id").map(Object::toString).orElse("TASK-UNKNOWN");
            String sessionId = state.value("session_id").map(Object::toString).orElse("SESSION-UNKNOWN");
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            var request = approvalService.createApprovalRequest(taskId, step.getAction(), step.getPurpose(), "default_user");
            String approvalId = request.getApprovalId();
            return StepResult.builder().success(true).result("已创建审批请求，审批ID: " + approvalId).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("审批创建失败: " + e.getMessage()).build();
        }
    }
}
