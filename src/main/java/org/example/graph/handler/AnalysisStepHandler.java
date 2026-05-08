package org.example.graph.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.agent.analysis.AnalysisAgent;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

public class AnalysisStepHandler implements PlanStepHandler {

    private final AnalysisAgent analysisAgent;
    private final String dashScopeApiKey;

    public AnalysisStepHandler(AnalysisAgent analysisAgent, String dashScopeApiKey) {
        this.analysisAgent = analysisAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public String agentType() {
        return "analysis";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            String result = analysisAgent.create(dashScopeApi).call(input).getText();
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("多维度分析失败: " + e.getMessage()).build();
        }
    }
}
