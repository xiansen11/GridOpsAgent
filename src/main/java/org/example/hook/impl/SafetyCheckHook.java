package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SafetyCheckHook implements AgentHook {

    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "停电", "停运", "紧急派单", "降负荷", "跳闸", "隔离"
    );

    @Override
    public String getName() { return "safety_check_hook"; }

    @Override
    public int getOrder() { return 20; }

    @Override
    public HookResult execute(HookContext context) {
        if (context.getOutput() == null) return HookResult.proceed();

        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (context.getOutput().contains(keyword)) {
                context.setParam("safetyWarning", true);
                context.setParam("safetyKeyword", keyword);
                break;
            }
        }
        return HookResult.proceed();
    }
}
