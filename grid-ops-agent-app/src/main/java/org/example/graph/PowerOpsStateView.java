package org.example.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PowerOpsStateView {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final OverAllState state;

    public PowerOpsStateView(OverAllState state) {
        this.state = state;
    }

    public String input() {
        return string(GraphStateKeys.INPUT);
    }

    public String cleanedInput() {
        return string(GraphStateKeys.CLEANED_INPUT);
    }

    public String taskId() {
        return string(GraphStateKeys.TASK_ID);
    }

    public String sessionId() {
        return string(GraphStateKeys.SESSION_ID);
    }

    public String traceId() {
        return string(GraphStateKeys.TRACE_ID);
    }

    public String evidence() {
        return string(GraphStateKeys.EVIDENCE);
    }

    public String executionResult() {
        return string(GraphStateKeys.EXECUTION_RESULT);
    }

    public String riskLevel() {
        String value = string(GraphStateKeys.RISK_LEVEL);
        return value.isBlank() ? "MEDIUM" : value;
    }

    public int loopCount() {
        return integer(GraphStateKeys.LOOP_COUNT, 0);
    }

    public List<PlanStep> planSteps() {
        return convertList(GraphStateKeys.PLAN_STEPS, new TypeReference<>() {});
    }

    public List<StepResult> stepResults() {
        return convertList(GraphStateKeys.STEP_RESULTS, new TypeReference<>() {});
    }

    public Map<String, Object> data() {
        return state.data();
    }

    public String string(String key) {
        return state.value(key).map(Object::toString).orElse("");
    }

    public int integer(String key, int defaultValue) {
        return state.value(key)
                .map(value -> {
                    if (value instanceof Number number) {
                        return number.intValue();
                    }
                    try {
                        return Integer.parseInt(value.toString());
                    } catch (Exception ignored) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private <T> List<T> convertList(String key, TypeReference<List<T>> typeReference) {
        Object value = state.value(key).orElse(null);
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return OBJECT_MAPPER.convertValue(list, typeReference);
    }
}
