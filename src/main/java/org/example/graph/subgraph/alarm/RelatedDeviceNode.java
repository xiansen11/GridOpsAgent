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

public class RelatedDeviceNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RelatedDeviceNode.class);
    private final String dashScopeApiKey;
    private final ToolCallbackProvider tools;

    public RelatedDeviceNode(String dashScopeApiKey, ToolCallbackProvider tools) {
        this.dashScopeApiKey = dashScopeApiKey;
        this.tools = tools;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String prevResult = state.value("execution_result").map(Object::toString).orElse("");
        logger.info("RelatedDeviceNode: 查询关联设备状态");

        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.1).withMaxToken(1000).build())
                    .build();
            ReactAgent agent = ReactAgent.builder()
                    .name("related_device_query")
                    .model(chatModel)
                    .systemPrompt("请调用getDeviceStatus工具查询关联设备状态。")
                    .tools(tools.getToolCallbacks())
                    .build();
            String result = agent.call("查询关联设备状态: " + input).getText();
            return Map.of("execution_result", prevResult + "\n\n--- 关联设备状态 ---\n" + result);
        } catch (Exception e) {
            return Map.of("execution_result", prevResult + "\n\n关联设备查询失败: " + e.getMessage());
        }
    }
}
