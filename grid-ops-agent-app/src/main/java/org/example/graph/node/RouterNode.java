package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.router.RouterAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RouterNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RouterNode.class);
    private final RouterAgent routerAgent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouterNode(RouterAgent routerAgent) {
        this.routerAgent = routerAgent;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String existingIntent = state.value("intent").map(Object::toString).orElse("");

        if (!existingIntent.isEmpty()) {
            logger.info("RouterNode: 使用预设意图 intent={}", existingIntent);
            return Map.of("intent", existingIntent, "confidence", 1.0);
        }

        logger.info("RouterNode: 意图识别, input={}", input);
        String routeResult = routerAgent.route(input);

        String intent = "GENERAL_CHAT";
        double confidence = 0.5;
        try {
            JsonNode node = objectMapper.readTree(routeResult);
            if (node.has("intent")) {
                intent = node.get("intent").asText();
            }
            if (node.has("confidence")) {
                confidence = node.get("confidence").asDouble();
            }
        } catch (Exception e) {
            logger.warn("意图解析失败，降级为GENERAL_CHAT: {}", e.getMessage());
        }

        logger.info("RouterNode: 意图识别结果 intent={}, confidence={}", intent, confidence);
        return Map.of("intent", intent, "confidence", confidence);
    }
}
