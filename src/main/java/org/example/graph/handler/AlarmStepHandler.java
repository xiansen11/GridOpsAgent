package org.example.graph.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.agent.alarm.AlarmAgent;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

public class AlarmStepHandler implements PlanStepHandler {

    private final AlarmAgent alarmAgent;
    private final String dashScopeApiKey;

    public AlarmStepHandler(AlarmAgent alarmAgent, String dashScopeApiKey) {
        this.alarmAgent = alarmAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public String agentType() {
        return "alarm";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            String result = alarmAgent.create(dashScopeApi).call(input).getText();
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("告警分析失败: " + e.getMessage()).build();
        }
    }
}
