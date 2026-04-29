package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.example.security.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HumanApprovalHook implements AgentHook {

    @Autowired
    private ApprovalService approvalService;

    @Override
    public String getName() { return "human_approval_hook"; }

    @Override
    public int getOrder() { return 50; }

    @Override
    public HookResult execute(HookContext context) {
        Boolean requireApproval = (Boolean) context.getParam("requireHumanApproval");
        if (!Boolean.TRUE.equals(requireApproval)) {
            return HookResult.proceed();
        }

        String approvalReason = (String) context.getParam("approvalReason");
        String taskId = context.getTaskId();
        String operation = context.getAgentName() != null ? context.getAgentName() : "unknown";

        ApprovalService.ApprovalRequest request = approvalService.createApprovalRequest(
                taskId, operation,
                approvalReason != null ? approvalReason : "高风险操作需要人工确认",
                "system");

        logger.warn("高风险操作已创建审批请求: approvalId={}, taskId={}, reason={}",
                request.getApprovalId(), taskId, approvalReason);

        context.setParam("approvalId", request.getApprovalId());
        context.setParam("approvalStatus", "PENDING");

        return HookResult.proceed();
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HumanApprovalHook.class);
}
