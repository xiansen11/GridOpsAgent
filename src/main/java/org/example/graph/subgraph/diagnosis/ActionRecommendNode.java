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
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String executionResult = state.value("execution_result").map(Object::toString).orElse("");
        String riskLevel = state.value("risk_level").map(Object::toString).orElse("MEDIUM");
        logger.info("ActionRecommendNode: 行动建议+结果汇总, riskLevel={}", riskLevel);

        StringBuilder result = new StringBuilder(executionResult);

        Object stepResultsObj = state.value("step_results").orElse(null);
        if (stepResultsObj instanceof List<?> stepResults && !stepResults.isEmpty()) {
            result.append("\n\n--- 执行过程摘要 ---\n");
            for (Object obj : stepResults) {
                if (obj instanceof Map<?, ?> m) {
                    result.append("步骤").append(m.get("step") != null ? m.get("step") : "?").append(" [")
                            .append(m.get("action") != null ? m.get("action") : "?").append("]: ")
                            .append(m.get("status") != null ? m.get("status") : "?").append("\n");
                }
            }
        }

        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            result.append("\n\n⚠️ **高风险操作提醒**：本诊断建议涉及高风险操作，请务必由专业人员确认后再执行。");
        }

        return Map.of("final_response", result.toString());
    }
}
