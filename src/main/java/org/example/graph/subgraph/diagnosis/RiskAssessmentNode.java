package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.agent.risk.RiskReviewAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RiskAssessmentNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentNode.class);
    private final RiskReviewAgent riskReviewAgent;
    private final String dashScopeApiKey;

    public RiskAssessmentNode(RiskReviewAgent riskReviewAgent, String dashScopeApiKey) {
        this.riskReviewAgent = riskReviewAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String diagnosis = state.value("execution_result").map(Object::toString).orElse("");
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("RiskAssessmentNode: 风险评估+行动建议");

        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        String riskInput = "原始问题: " + input + "\n\n诊断结果:\n" + diagnosis;
        String result = riskReviewAgent.create(dashScopeApi).call(riskInput).getText();

        String riskLevel = "MEDIUM";
        if (result.contains("CRITICAL")) riskLevel = "CRITICAL";
        else if (result.contains("HIGH")) riskLevel = "HIGH";
        else if (result.contains("LOW")) riskLevel = "LOW";

        return Map.of("risk_level", riskLevel, "execution_result", result);
    }
}
