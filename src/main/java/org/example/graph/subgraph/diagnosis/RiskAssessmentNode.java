package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class RiskAssessmentNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentNode.class);
    private final ChatClient chatClient;

    public RiskAssessmentNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String diagnosis = state.value("execution_result").map(Object::toString).orElse("");
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("RiskAssessmentNode: 风险评估");

        String prompt = String.format(
                "评估以下诊断建议的风险等级。只返回一个风险等级：LOW/MEDIUM/HIGH/CRITICAL\n\n" +
                "诊断建议: %s\n\n原始问题: %s", diagnosis, input);

        String riskLevel = chatClient.prompt().user(prompt).call().content().trim().toUpperCase();
        if (!riskLevel.matches("LOW|MEDIUM|HIGH|CRITICAL")) {
            riskLevel = "MEDIUM";
        }

        return Map.of("risk_level", riskLevel, "execution_result", diagnosis);
    }
}
