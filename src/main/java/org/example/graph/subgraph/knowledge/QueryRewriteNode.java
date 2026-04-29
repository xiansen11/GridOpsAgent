package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class QueryRewriteNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(QueryRewriteNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String memoryContext = state.value("memory_context").map(Object::toString).orElse("");

        logger.info("QueryRewriteNode: Query改写, input={}", input);

        String rewritten = input;
        if (input.contains("油温高") && !input.contains("变压器")) {
            rewritten = "变压器" + input;
        }
        if (input.contains("局放") && !input.contains("开关柜")) {
            rewritten = "开关柜" + input;
        }
        if ((input.contains("它") || input.contains("该设备")) && !memoryContext.isEmpty()) {
            if (memoryContext.contains("TR-110KV")) {
                rewritten = rewritten.replace("它", "TR-110KV-001主变").replace("该设备", "TR-110KV-001主变");
            }
        }

        return Map.of("cleaned_input", rewritten);
    }
}
