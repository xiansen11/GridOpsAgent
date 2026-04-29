package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

@Component
public class PreDiagnosisHook implements AgentHook {

    @Override
    public String getName() { return "pre_diagnosis_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        Boolean ragQualityLow = (Boolean) context.getParam("ragQualityLow");
        Boolean toolResultsEmpty = (Boolean) context.getParam("toolResultsEmpty");

        if (Boolean.TRUE.equals(ragQualityLow) && Boolean.TRUE.equals(toolResultsEmpty)) {
            context.setParam("diagnosisWarning", "知识库检索和工具调用结果均不足，诊断结论可能不够准确");
        }

        return HookResult.proceed();
    }
}
