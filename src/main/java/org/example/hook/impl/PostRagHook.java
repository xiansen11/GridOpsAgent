package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

@Component
public class PostRagHook implements AgentHook {

    @Override
    public String getName() { return "post_rag_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String ragResults = context.getOutput();
        if (ragResults == null || ragResults.trim().isEmpty() || ragResults.equals("[]")) {
            context.setParam("ragQualityLow", true);
        }
        return HookResult.proceed();
    }
}
