package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PostRouteHook implements AgentHook {

    private static final Set<String> VALID_INTENTS = Set.of(
            "SAFETY_QA", "DEVICE_STATUS", "ALARM_QUERY", "DEVICE_PROFILE",
            "LOG_ANALYSIS", "TICKET_QUERY", "FAULT_DIAGNOSIS", "ALARM_DIAGNOSIS",
            "GENERAL_CHAT"
    );

    @Override
    public String getName() { return "post_route_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String intent = context.getOutput();
        if (intent == null || !VALID_INTENTS.contains(intent)) {
            context.setOutput("GENERAL_CHAT");
        }
        return HookResult.proceed();
    }
}
