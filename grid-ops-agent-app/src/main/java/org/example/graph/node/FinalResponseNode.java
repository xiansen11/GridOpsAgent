package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FinalResponseNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(FinalResponseNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String response = state.value("final_response").map(Object::toString).orElse("");

        logger.info("FinalResponseNode: 格式化最终响应");

        Object stepResultsObj = state.value("step_results").orElse(null);
        List<?> stepResults = new ArrayList<>();
        if (stepResultsObj instanceof List) {
            stepResults = (List<?>) stepResultsObj;
        }

        if (!stepResults.isEmpty() && !response.contains("执行过程摘要")) {
            StringBuilder sb = new StringBuilder(response);
            sb.append("\n\n--- 执行过程摘要 ---\n");
            for (Object obj : stepResults) {
                if (obj instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) obj;
                    Object stepVal = m.get("step");
                    Object actionVal = m.get("action");
                    Object statusVal = m.get("status");
                    sb.append("步骤").append(stepVal != null ? stepVal : "?").append(" [")
                            .append(actionVal != null ? actionVal : "?").append("]: ")
                            .append(statusVal != null ? statusVal : "?").append("\n");
                }
            }
            response = sb.toString();
        }

        return Map.of("final_response", response);
    }
}
