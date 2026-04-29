package org.example.graph.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.agent.knowledge.KnowledgeAgent;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

public class KnowledgeStepHandler implements PlanStepHandler {

    private final KnowledgeAgent knowledgeAgent;
    private final String dashScopeApiKey;

    public KnowledgeStepHandler(KnowledgeAgent knowledgeAgent, String dashScopeApiKey) {
        this.knowledgeAgent = knowledgeAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public String agentType() {
        return "knowledge";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            String result = knowledgeAgent.create(dashScopeApi).call(input).getText();
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("知识问答失败: " + e.getMessage()).build();
        }
    }
}
