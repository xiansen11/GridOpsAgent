package org.example.graph.subgraph.dynamic;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class DynamicReplannerNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(DynamicReplannerNode.class);
    private static final int MAX_LOOP = 5;
    private final ChatClient chatClient;

    public DynamicReplannerNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String planJson = state.value("plan").map(Object::toString).orElse("");
        int currentIndex = state.value("current_step_index").map(v -> Integer.parseInt(v.toString())).orElse(0);
        int loopCount = state.value("loop_count").map(v -> Integer.parseInt(v.toString())).orElse(0);
        String executionResult = state.value("execution_result").map(Object::toString).orElse("");

        logger.info("DynamicReplannerNode: loopCount={}, currentIndex={}", loopCount, currentIndex);

        if (loopCount >= MAX_LOOP) {
            logger.info("达到最大循环次数，降级处理");
            return Map.of("next_action", "FALLBACK");
        }

        if (planJson.contains("\"steps\":[]") || !planJson.contains("steps")) {
            return Map.of("next_action", "END");
        }

        try {
            var stepsNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(planJson).get("steps");
            if (stepsNode == null || currentIndex >= stepsNode.size()) {
                return Map.of("next_action", "END");
            }
        } catch (Exception e) {
            return Map.of("next_action", "END");
        }

        if (executionResult.contains("失败") || executionResult.contains("错误")) {
            if (loopCount >= MAX_LOOP - 1) {
                logger.info("连续执行失败且接近最大循环次数，需要重新规划");
                return Map.of("next_action", "REPLAN", "loop_count", loopCount + 1);
            }
            return Map.of("next_action", "CONTINUE", "loop_count", loopCount + 1);
        }

        String riskLevel = state.value("risk_level").map(Object::toString).orElse("");
        if ("CRITICAL".equals(riskLevel)) {
            return Map.of("next_action", "HUMAN_APPROVAL", "loop_count", loopCount + 1);
        }

        return Map.of("next_action", "CONTINUE", "loop_count", loopCount + 1);
    }
}
