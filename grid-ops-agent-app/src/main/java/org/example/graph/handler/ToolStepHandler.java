package org.example.graph.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.agent.tool_agent.ToolAgent;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

public class ToolStepHandler implements PlanStepHandler {

    private final ToolAgent toolAgent;
    private final String dashScopeApiKey;

    public ToolStepHandler(ToolAgent toolAgent, String dashScopeApiKey) {
        this.toolAgent = toolAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public String agentType() {
        return "tool";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            String toolPrompt = String.format("请调用工具 %s 完成以下任务: %s", step.getAction(), step.getPurpose());
            String result = toolAgent.create(dashScopeApi).call(toolPrompt).getText();
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("工具调用失败: " + e.getMessage()).build();
        }
    }
}
