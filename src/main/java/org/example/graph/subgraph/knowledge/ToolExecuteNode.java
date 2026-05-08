package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.agent.tool_agent.ToolAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ToolExecuteNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ToolExecuteNode.class);
    private final ToolAgent toolAgent;
    private final String dashScopeApiKey;

    public ToolExecuteNode(ToolAgent toolAgent, String dashScopeApiKey) {
        this.toolAgent = toolAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        Object entitiesObj = state.value("entities").orElse(Map.of());
        logger.info("ToolExecuteNode: ToolAgent工具调用");

        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            String result = toolAgent.create(dashScopeApi).call(input).getText();
            return Map.of("tool_result", result);
        } catch (Exception e) {
            logger.error("ToolExecuteNode: 工具调用失败", e);
            return Map.of("tool_result", "工具调用失败: " + e.getMessage());
        }
    }
}
