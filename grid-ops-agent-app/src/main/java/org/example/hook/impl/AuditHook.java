package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditHook implements AgentHook {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOG");

    @Override
    public String getName() { return "audit_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        auditLogger.info("[AUDIT] agent={}, hookPoint={}, sessionId={}, taskId={}, inputLength={}",
                context.getAgentName(),
                context.getHookPoint(),
                context.getSessionId(),
                context.getTaskId(),
                context.getInput() != null ? context.getInput().length() : 0);
        return HookResult.proceed();
    }
}
