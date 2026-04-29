package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class DiagnosisReplannerNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosisReplannerNode.class);
    private static final int MAX_LOOP = 2;
    private final ChatClient chatClient;

    public DiagnosisReplannerNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String diagnosis = state.value("execution_result").map(Object::toString).orElse("");
        String evidence = state.value("evidence").map(v -> v.toString()).orElse("");
        int loopCount = state.value("loop_count").map(v -> Integer.parseInt(v.toString())).orElse(0);

        logger.info("DiagnosisReplannerNode: 重规划决策, loopCount={}", loopCount);

        if (loopCount >= MAX_LOOP) {
            if (evidence.contains("分析失败") || evidence.length() < 50) {
                logger.info("达到最大循环次数且证据仍不足，降级处理");
                return Map.of("next_action", "FALLBACK");
            }
            logger.info("达到最大循环次数，继续执行");
            return Map.of("next_action", "CONTINUE");
        }

        if (evidence.contains("分析失败") || evidence.length() < 50) {
            logger.info("证据不足，需要补充调查");
            return Map.of("next_action", "REPLAN", "loop_count", loopCount + 1);
        }

        String riskLevel = state.value("risk_level").map(Object::toString).orElse("MEDIUM");
        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            return Map.of("next_action", "HUMAN_APPROVAL");
        }

        return Map.of("next_action", "CONTINUE");
    }
}
