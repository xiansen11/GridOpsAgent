package org.example.graph.subgraph.alarm;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Map;

public class AlarmHistoryNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AlarmHistoryNode.class);
    private final String dashScopeApiKey;
    private final ToolCallbackProvider tools;

    public AlarmHistoryNode(String dashScopeApiKey, ToolCallbackProvider tools) {
        this.dashScopeApiKey = dashScopeApiKey;
        this.tools = tools;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("AlarmHistoryNode: 查询告警历史");

        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.1).withMaxToken(1000).build())
                    .build();
            ReactAgent agent = ReactAgent.builder()
                    .name("alarm_history_query")
                    .model(chatModel)
                    .systemPrompt("请调用getAlarmHistory工具查询历史告警记录。")
                    .tools(tools.getToolCallbacks())
                    .build();
            String result = agent.call("查询相关告警历史: " + input).getText();
            return Map.of("execution_result", result);
        } catch (Exception e) {
            return Map.of("execution_result", "告警历史查询失败: " + e.getMessage());
        }
    }
}
