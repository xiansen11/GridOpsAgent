package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActionRecommendNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ActionRecommendNode.class);

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String executionResult = state.value("execution_result").map(Object::toString).orElse("");
        String riskLevel = state.value("risk_level").map(Object::toString).orElse("MEDIUM");
        String evidence = state.value("evidence").map(Object::toString).orElse("");
        String diagnosisResult = state.value("diagnosis_result").map(Object::toString).orElse("");
        logger.info("ActionRecommendNode: 行动建议+结果汇总, riskLevel={}", riskLevel);

        StringBuilder result = new StringBuilder();

        if (!diagnosisResult.isEmpty()) {
            result.append(diagnosisResult);
        } else {
            result.append(executionResult);
        }

        Object stepResultsObj = state.value("step_results").orElse(null);
        List<Map<String, Object>> stepResults = new ArrayList<>();
        if (stepResultsObj instanceof List<?> list && !list.isEmpty()) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> m) {
                    stepResults.add((Map<String, Object>) m);
                }
            }
        }

        if (!stepResults.isEmpty()) {
            result.append("\n\n--- 执行过程摘要 ---\n");
            for (Map<String, Object> step : stepResults) {
                result.append("步骤").append(step.getOrDefault("step", "?")).append(" [")
                        .append(step.getOrDefault("action", "?")).append("]: ")
                        .append(step.getOrDefault("status", "?")).append("\n");
                if (step.containsKey("result")) {
                    result.append("  结果: ").append(step.get("result")).append("\n");
                }
            }
        } else if (!evidence.isEmpty() && !evidence.equals(executionResult)) {
            result.append("\n\n--- 证据采集摘要 ---\n").append(evidence, 0, Math.min(evidence.length(), 500));
            if (evidence.length() > 500) {
                result.append("...(已截断)");
            }
        }

        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            result.append("\n\n⚠️ **高风险操作提醒**：本诊断建议涉及高风险操作，请务必由专业人员确认后再执行。");
            result.append("\n⚠️ 建议启动人工审批流程，确认后方可执行相关操作。");
        }

        return Map.of("final_response", result.toString());
    }
}
