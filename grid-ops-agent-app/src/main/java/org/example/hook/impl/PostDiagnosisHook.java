package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PostDiagnosisHook implements AgentHook {

    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "停电", "停运", "紧急派单", "降负荷", "跳闸", "隔离", "远程控制"
    );

    @Override
    public String getName() { return "post_diagnosis_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String diagnosis = context.getOutput();
        if (diagnosis == null) {
            return HookResult.proceed();
        }

        boolean hasHighRiskSuggestion = false;
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (diagnosis.contains(keyword)) {
                hasHighRiskSuggestion = true;
                context.setParam("highRiskKeyword", keyword);
                break;
            }
        }

        if (hasHighRiskSuggestion) {
            context.setParam("requireHumanApproval", true);
            context.setParam("approvalReason", "诊断建议包含高风险操作，需要人工确认");
        }

        boolean hasSafetyWarning = diagnosis.contains("安全") || diagnosis.contains("规程") || diagnosis.contains("⚠️");
        if (!hasSafetyWarning) {
            context.setOutput(diagnosis + "\n\n⚠️ 安全提示：涉及设备操作时，请严格遵守现场安全规程，高风险操作需经值班负责人确认。");
        }

        return HookResult.proceed();
    }
}
