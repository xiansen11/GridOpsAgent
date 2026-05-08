package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.agent.analysis.AnalysisAgent;
import org.example.agent.tool_agent.ToolAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EvidenceCollectNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceCollectNode.class);
    private final AnalysisAgent analysisAgent;
    private final ToolAgent toolAgent;
    private final String dashScopeApiKey;

    public EvidenceCollectNode(AnalysisAgent analysisAgent, ToolAgent toolAgent, String dashScopeApiKey) {
        this.analysisAgent = analysisAgent;
        this.toolAgent = toolAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String intent = state.value("intent").map(Object::toString).orElse("DIAGNOSIS");
        String alarmLevel = state.value("alarm_level").map(Object::toString).orElse("一般");

        logger.info("EvidenceCollectNode: 自适应证据采集, intent={}, alarmLevel={}", intent, alarmLevel);

        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        String result;

        if ("紧急".equals(alarmLevel) || "重要".equals(alarmLevel) || "DIAGNOSIS".equals(intent)) {
            logger.info("使用AnalysisAgent进行全面分析");
            result = analysisAgent.create(dashScopeApi).call(input).getText();
        } else {
            logger.info("使用ToolAgent进行轻量查询");
            result = toolAgent.create(dashScopeApi).call(input).getText();
        }

        return Map.of("evidence", result, "execution_result", result);
    }
}
