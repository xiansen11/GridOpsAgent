package org.example.graph.subgraph.device;

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

public class ToolExecuteNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ToolExecuteNode.class);
    private final String dashScopeApiKey;
    private final ToolCallbackProvider tools;

    public ToolExecuteNode(String dashScopeApiKey, ToolCallbackProvider tools) {
        this.dashScopeApiKey = dashScopeApiKey;
        this.tools = tools;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String toolName = state.value("execution_result").map(Object::toString).orElse("getDeviceStatus");
        Object entitiesObj = state.value("entities").orElse(Map.of());
        Map<?, ?> entities = (entitiesObj instanceof Map) ? (Map<?, ?>) entitiesObj : Map.of();
        Object deviceIdObj = entities.get("deviceId");
        String deviceId = deviceIdObj != null ? deviceIdObj.toString() : "UNKNOWN";
        String input = state.value("cleaned_input").map(Object::toString).orElse("");

        logger.info("ToolExecuteNode: 工具执行, toolName={}, deviceId={}", toolName, deviceId);

        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.1).withMaxToken(1000).build())
                    .build();

            String toolPrompt = String.format("请调用工具 %s 查询设备 %s 的信息。用户原始问题: %s", toolName, deviceId, input);
            ReactAgent agent = ReactAgent.builder()
                    .name("tool_executor")
                    .model(chatModel)
                    .systemPrompt("你是工具调用助手，请调用指定工具完成任务。")
                    .tools(tools.getToolCallbacks())
                    .build();

            String result = agent.call(toolPrompt).getText();
            return Map.of("execution_result", result);
        } catch (Exception e) {
            return Map.of("execution_result", "工具执行失败: " + e.getMessage());
        }
    }
}
