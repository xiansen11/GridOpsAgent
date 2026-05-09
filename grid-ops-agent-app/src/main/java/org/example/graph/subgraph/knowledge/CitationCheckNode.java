package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CitationCheckNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(CitationCheckNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String response = state.value("final_response").map(Object::toString).orElse("");
        String ragContext = state.value("execution_result").map(Object::toString).orElse("");

        logger.info("CitationCheckNode: 引用检查");

        if (ragContext.isEmpty() || ragContext.length() < 20) {
            response += "\n\n⚠️ 注意：本回答未找到充分的参考资料支撑，建议核实相关信息。";
        }

        return Map.of("final_response", response);
    }
}
