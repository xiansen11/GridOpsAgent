package org.example.graph.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;
import org.springframework.ai.tool.ToolCallbackProvider;

public class ToolStepHandler implements PlanStepHandler {

    private final String dashScopeApiKey;
    private final ToolCallbackProvider tools;

    public ToolStepHandler(String dashScopeApiKey, ToolCallbackProvider tools) {
        this.dashScopeApiKey = dashScopeApiKey;
        this.tools = tools;
    }

    @Override
    public String agentType() {
        return "tool";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.1).withMaxToken(1000).build())
                    .build();
            String toolPrompt = String.format("请调用工具 %s 完成以下任务: %s", step.getAction(), step.getPurpose());
            ReactAgent agent = ReactAgent.builder()
                    .name("tool_executor")
                    .model(chatModel)
                    .systemPrompt("你是工具调用助手，请调用指定工具完成任务。")
                    .tools(tools.getToolCallbacks())
                    .build();
            String result = agent.call(toolPrompt).getText();
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("工具调用失败: " + e.getMessage()).build();
        }
    }
}
