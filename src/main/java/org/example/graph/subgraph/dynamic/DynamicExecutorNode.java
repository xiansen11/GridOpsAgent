package org.example.graph.subgraph.dynamic;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.graph.handler.StepHandlerRegistry;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicExecutorNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(DynamicExecutorNode.class);
    private final StepHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DynamicExecutorNode(StepHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String planJson = state.value("plan").map(Object::toString).orElse("");
        int currentIndex = state.value("current_step_index").map(v -> Integer.parseInt(v.toString())).orElse(0);

        List<PlanStep> steps = parsePlan(planJson);
        if (steps.isEmpty() || currentIndex >= steps.size()) {
            return Map.of("final_response", "执行计划为空或已完成");
        }

        PlanStep currentStep = steps.get(currentIndex);
        logger.info("DynamicExecutorNode: 执行步骤 {}/{}, action={}, agentType={}",
                currentIndex + 1, steps.size(), currentStep.getAction(), currentStep.getAgentType());

        StepResult result = handlerRegistry.getHandler(currentStep.getAgentType()).execute(currentStep, state);

        Map<String, String> stepResultEntry = Map.of(
                "step", String.valueOf(currentStep.getStep()),
                "action", currentStep.getAction(),
                "status", result.isSuccess() ? "COMPLETED" : "FAILED",
                "result", result.getResult() != null ? result.getResult() : result.getError()
        );

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("execution_result", result.getResult() != null ? result.getResult() : result.getError());
        output.put("current_step_index", currentIndex + 1);
        output.put("step_results", List.of(stepResultEntry));
        return output;
    }

    private List<PlanStep> parsePlan(String planJson) {
        try {
            Map<String, List<PlanStep>> map = objectMapper.readValue(planJson,
                    new TypeReference<Map<String, List<PlanStep>>>() {});
            return map.getOrDefault("steps", List.of());
        } catch (Exception e) {
            logger.error("计划解析失败: {}", e.getMessage());
            return List.of();
        }
    }
}
