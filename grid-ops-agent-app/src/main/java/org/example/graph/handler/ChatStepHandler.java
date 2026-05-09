package org.example.graph.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;
import org.springframework.ai.tool.ToolCallbackProvider;

public class ChatStepHandler implements PlanStepHandler {

    private final String dashScopeApiKey;
    private final ToolCallbackProvider tools;

    public ChatStepHandler(String dashScopeApiKey, ToolCallbackProvider tools) {
        this.dashScopeApiKey = dashScopeApiKey;
        this.tools = tools;
    }

    @Override
    public String agentType() {
        return "chat";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            String skillContext = state.value("skill_context").map(Object::toString).orElse("");
            String memoryContext = state.value("memory_context").map(Object::toString).orElse("");

            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是电力智能运维平台的智能助手，专门面向电力巡检、监控告警处理、设备故障排障和现场知识查询场景。\n");
            systemPrompt.append("重要安全规则：涉及安全操作时必须提示遵守现场规程；严禁编造数据；高风险建议必须标注并建议人工确认。\n\n");
            if (!skillContext.isEmpty()) {
                systemPrompt.append("--- 当前业务场景指导 ---\n").append(skillContext).append("\n\n");
            }
            if (!memoryContext.isEmpty()) {
                systemPrompt.append("--- 上下文记忆 ---\n").append(memoryContext).append("\n\n");
            }

            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.7).withMaxToken(2000).withTopP(0.9).build())
                    .build();

            ReactAgent agent = ReactAgent.builder()
                    .name("power_chat_agent")
                    .model(chatModel)
                    .systemPrompt(systemPrompt.toString())
                    .tools(tools.getToolCallbacks())
                    .build();

            String result = agent.call(input).getText();
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("对话处理失败: " + e.getMessage()).build();
        }
    }
}
