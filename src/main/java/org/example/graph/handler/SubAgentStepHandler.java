package org.example.graph.handler;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.agent.subagent.SubagentExecutor;
import org.example.agent.subagent.SubagentTask;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

import java.util.List;
import java.util.Map;

public class SubAgentStepHandler implements PlanStepHandler {

    private final SubagentExecutor subagentExecutor;

    public SubAgentStepHandler(SubagentExecutor subagentExecutor) {
        this.subagentExecutor = subagentExecutor;
    }

    @Override
    public String agentType() {
        return "subagents";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            Map<String, Object> params = step.getParams() != null ? step.getParams() : Map.of();
            Object namesObj = params.get("subagentNames");
            List<String> names;
            if (namesObj instanceof List) {
                names = ((List<?>) namesObj).stream().map(Object::toString).toList();
            } else {
                names = List.of("regulation", "metrics", "log", "ticket");
            }
            List<SubagentTask> tasks = names.stream()
                    .map(name -> SubagentTask.builder().subagentName(name).input(input).build())
                    .toList();
            List<SubagentTask> results = subagentExecutor.executeParallel(tasks);
            StringBuilder sb = new StringBuilder();
            for (SubagentTask task : results) {
                sb.append("## ").append(task.getSubagentName()).append(" 分析结果\n");
                sb.append(task.getResult() != null ? task.getResult() : "分析失败").append("\n\n");
            }
            return StepResult.builder().success(true).result(sb.toString()).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("子Agent执行失败: " + e.getMessage()).build();
        }
    }
}
