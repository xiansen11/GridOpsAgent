package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.example.graph.GraphStateKeys;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;
import org.example.graph.validation.ToolResultValidator;
import org.example.graph.validation.ValidationResult;
import org.example.observability.ObservabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutorNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorNode.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ToolCallbackProvider tools;
    private final RetryRegistry retryRegistry;
    private final ToolResultValidator toolResultValidator;
    private final ObservabilityService observabilityService;

    public ExecutorNode(ToolCallbackProvider tools, RetryRegistry retryRegistry,
                        ToolResultValidator toolResultValidator,
                        ObservabilityService observabilityService) {
        this.tools = tools;
        this.retryRegistry = retryRegistry;
        this.toolResultValidator = toolResultValidator;
        this.observabilityService = observabilityService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        List<PlanStep> planSteps = readSteps(state.value(GraphStateKeys.PLAN_STEPS).orElse(List.of()));
        appendAdditionalSteps(planSteps, state.value(GraphStateKeys.ADDITIONAL_STEPS).orElse(List.of()));
        if (planSteps.isEmpty()) {
            logger.warn("ExecutorNode: no plan steps, skipping execution");
            return Map.of(GraphStateKeys.EVIDENCE, "", GraphStateKeys.EXECUTION_RESULT, "", GraphStateKeys.STEP_RESULTS, List.of());
        }

        Map<String, ToolCallback> callbackMap = Arrays.stream(tools.getToolCallbacks())
                .collect(Collectors.toMap(callback -> callback.getToolDefinition().name(), Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<StepResult> newResults = new ArrayList<>();
        StringBuilder evidenceBuilder = new StringBuilder(state.value(GraphStateKeys.EVIDENCE).map(Object::toString).orElse(""));
        String taskId = state.value(GraphStateKeys.TASK_ID).map(Object::toString).orElse(null);
        String sessionId = state.value(GraphStateKeys.SESSION_ID).map(Object::toString).orElse(null);
        String traceId = state.value(GraphStateKeys.TRACE_ID).map(Object::toString).orElseGet(observabilityService::generateTraceId);

        for (PlanStep step : planSteps) {
            if ("COMPLETED".equals(step.effectiveStatus()) || "SKIPPED".equals(step.effectiveStatus())) {
                continue;
            }
            if (!"TOOL_CALL".equals(step.effectiveStepType())) {
                step.setStatus("SKIPPED");
                continue;
            }

            StepResult stepResult = executeToolStep(step, callbackMap, traceId, taskId, sessionId);
            newResults.add(stepResult);
            step.setStatus(stepResult.isSuccess() ? "COMPLETED" : "FAILED");
            step.setRetryCount(stepResult.getRetryCount());
            step.setResult(stepResult.getResult());

            evidenceBuilder.append("## Step ").append(step.effectiveStepNo()).append(": ")
                    .append(step.getAction()).append("\n")
                    .append("Tool: ").append(step.effectiveToolName()).append("\n")
                    .append("Purpose: ").append(step.getPurpose()).append("\n")
                    .append("Status: ").append(stepResult.getStatus()).append("\n")
                    .append("Result: ").append(stepResult.getResult()).append("\n\n");
        }

        boolean requiredFailure = newResults.stream().anyMatch(result -> !result.isSuccess()
                && planSteps.stream().filter(PlanStep::effectiveRequired)
                .anyMatch(step -> step.effectiveStepId().equals(result.getStepId())));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(GraphStateKeys.EVIDENCE, evidenceBuilder.toString());
        output.put(GraphStateKeys.EXECUTION_RESULT, evidenceBuilder.toString());
        output.put(GraphStateKeys.STEP_RESULTS, newResults);
        output.put(GraphStateKeys.PLAN_STEPS, planSteps);
        if (requiredFailure) {
            output.put(GraphStateKeys.NEXT_ACTION, "REPLAN");
        }
        logger.info("ExecutorNode: executed {} new steps, requiredFailure={}", newResults.size(), requiredFailure);
        return output;
    }

    private StepResult executeToolStep(PlanStep step, Map<String, ToolCallback> callbackMap,
                                       String traceId, String taskId, String sessionId) {
        String toolName = step.effectiveToolName();
        String toolInput = toJson(step.getParams() == null ? Map.of() : step.getParams());
        ToolCallback callback = callbackMap.get(toolName);
        long start = System.currentTimeMillis();

        StepResult.StepResultBuilder result = StepResult.builder()
                .stepId(step.effectiveStepId())
                .stepNo(step.effectiveStepNo())
                .action(step.getAction())
                .toolName(toolName)
                .retryCount(0)
                .evidenceType(toolResultValidator.evidenceType(toolName));

        if (callback == null) {
            String message = "Tool not found: " + toolName;
            logTool(traceId, taskId, sessionId, toolName, toolInput, message, "FAILED", start);
            return result.status("FAILED")
                    .success(false)
                    .result(message)
                    .error(message)
                    .errorType("TOOL_NOT_FOUND")
                    .recoverable(true)
                    .nextSuggestion("REPLAN")
                    .matchExpected(false)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            Retry retry = retryRegistry.retry(toolName.startsWith("get") ? "mcpTool" : "llmCall");
            Callable<String> decorated = Retry.decorateCallable(retry, () -> callback.call(toolInput));
            String response = decorated.call();
            ValidationResult validation = toolResultValidator.validate(toolName, response);
            long duration = System.currentTimeMillis() - start;
            if (!validation.isValid()) {
                logTool(traceId, taskId, sessionId, toolName, toolInput, response, "FAILED", start);
                return result.status("FAILED")
                        .success(false)
                        .result(response)
                        .error(String.join("; ", validation.getErrors()))
                        .errorType(validation.getErrorType())
                        .recoverable(validation.isRecoverable())
                        .nextSuggestion(validation.isRecoverable() ? "REPLAN" : "STOP")
                        .matchExpected(false)
                        .durationMs(duration)
                        .build();
            }
            logTool(traceId, taskId, sessionId, toolName, toolInput, response, "SUCCESS", start);
            return result.status("COMPLETED")
                    .success(true)
                    .result(response)
                    .recoverable(false)
                    .matchExpected(true)
                    .durationMs(duration)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            String errorType = classify(e);
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            logTool(traceId, taskId, sessionId, toolName, toolInput, message, "FAILED", start);
            return result.status("FAILED")
                    .success(false)
                    .result("Tool execution failed: " + message)
                    .error(message)
                    .errorType(errorType)
                    .recoverable(!"TOOL_UNAUTHORIZED".equals(errorType))
                    .nextSuggestion("REPLAN")
                    .matchExpected(false)
                    .durationMs(duration)
                    .build();
        }
    }

    private List<PlanStep> readSteps(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return OBJECT_MAPPER.convertValue(list, new TypeReference<>() {});
    }

    private void appendAdditionalSteps(List<PlanStep> planSteps, Object additionalStepsObj) {
        if (!(additionalStepsObj instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        List<PlanStep> additionalSteps = OBJECT_MAPPER.convertValue(list, new TypeReference<>() {});
        int nextNo = planSteps.stream().mapToInt(PlanStep::effectiveStepNo).max().orElse(0) + 1;
        for (PlanStep step : additionalSteps) {
            step.setStepNo(nextNo);
            step.setStep(nextNo);
            step.setStepId(String.format("step-%03d", nextNo));
            step.setStatus("PENDING");
            step.setRetryCount(0);
            planSteps.add(step);
            nextNo++;
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String classify(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("timeout") || message.contains("timed out")) {
            return "TOOL_TIMEOUT";
        }
        if (message.contains("connection") || message.contains("connect")) {
            return "TOOL_CONNECTION_ERROR";
        }
        if (message.contains("unauthorized") || message.contains("forbidden")) {
            return "TOOL_UNAUTHORIZED";
        }
        return "TOOL_EXECUTION_ERROR";
    }

    private void logTool(String traceId, String taskId, String sessionId, String toolName,
                         String request, String response, String status, long start) {
        observabilityService.logToolCall(traceId, taskId, sessionId, toolName, request, response, status,
                System.currentTimeMillis() - start);
    }
}
