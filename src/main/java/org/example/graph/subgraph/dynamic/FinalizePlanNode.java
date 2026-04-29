package org.example.graph.subgraph.dynamic;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FinalizePlanNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(FinalizePlanNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String lastResult = state.value("execution_result").map(Object::toString).orElse("");
        logger.info("FinalizePlanNode: 汇总结果");

        Object stepResultsObj = state.value("step_results").orElse(null);
        List<?> stepResults = new ArrayList<>();
        if (stepResultsObj instanceof List) {
            stepResults = (List<?>) stepResultsObj;
        }

        StringBuilder sb = new StringBuilder(lastResult);
        if (!stepResults.isEmpty()) {
            sb.append("\n\n--- 执行过程摘要 ---\n");
            for (Object obj : stepResults) {
                if (obj instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) obj;
                    sb.append("步骤").append(m.get("step") != null ? m.get("step") : "?").append(" [")
                            .append(m.get("action") != null ? m.get("action") : "?").append("]: ")
                            .append(m.get("status") != null ? m.get("status") : "?").append("\n");
                }
            }
        }

        return Map.of("final_response", sb.toString());
    }
}
