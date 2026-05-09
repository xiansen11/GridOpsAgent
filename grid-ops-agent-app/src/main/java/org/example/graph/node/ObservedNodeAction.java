package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.checkpoint.CheckpointService;
import org.example.graph.GraphStateKeys;
import org.example.observability.ObservabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class ObservedNodeAction implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ObservedNodeAction.class);

    private final String nodeName;
    private final NodeAction delegate;
    private final ObservabilityService observabilityService;
    private final CheckpointService checkpointService;

    public ObservedNodeAction(String nodeName, NodeAction delegate,
                              ObservabilityService observabilityService,
                              CheckpointService checkpointService) {
        this.nodeName = nodeName;
        this.delegate = delegate;
        this.observabilityService = observabilityService;
        this.checkpointService = checkpointService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String traceId = state.value(GraphStateKeys.TRACE_ID).map(Object::toString)
                .orElseGet(observabilityService::generateTraceId);
        String taskId = state.value(GraphStateKeys.TASK_ID).map(Object::toString).orElse(null);
        String sessionId = state.value(GraphStateKeys.SESSION_ID).map(Object::toString).orElse(null);
        String input = state.value(GraphStateKeys.CLEANED_INPUT).map(Object::toString)
                .orElse(state.value(GraphStateKeys.INPUT).map(Object::toString).orElse(""));

        observabilityService.startSpan(traceId, taskId, sessionId, nodeName, nodeName, input);
        try {
            Map<String, Object> output = new LinkedHashMap<>(delegate.apply(state));
            if (state.value(GraphStateKeys.TRACE_ID).isEmpty()) {
                output.put(GraphStateKeys.TRACE_ID, traceId);
            }
            observabilityService.endSpan("SUCCESS", summarize(output), null);
            saveCheckpointIfNeeded(taskId, sessionId, state, output, null);
            return output;
        } catch (Exception e) {
            observabilityService.endSpan("FAILED", e.getMessage(), null);
            saveCheckpointIfNeeded(taskId, sessionId, state, Map.of(), e.getMessage());
            logger.error("ObservedNodeAction: node {} failed", nodeName, e);
            throw e;
        }
    }

    private void saveCheckpointIfNeeded(String taskId, String sessionId, OverAllState state,
                                        Map<String, Object> output, String errorMessage) {
        if (taskId == null || !shouldCheckpoint(nodeName)) {
            return;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>(state.data());
        snapshot.putAll(output);
        if (errorMessage != null) {
            snapshot.put("error_message", errorMessage);
        }
        checkpointService.saveCheckpoint(taskId, sessionId, checkpointName(nodeName), snapshot);
    }

    private boolean shouldCheckpoint(String node) {
        return switch (node) {
            case "entity_extract", "alarm_rag_retrieve", "planner", "executor",
                    "evidence_validation", "diagnosis", "risk_assessment", "final_response" -> true;
            default -> false;
        };
    }

    private String checkpointName(String node) {
        return switch (node) {
            case "entity_extract" -> "AFTER_ENTITY_EXTRACT";
            case "alarm_rag_retrieve" -> "AFTER_RAG_RETRIEVE";
            case "planner" -> "AFTER_PLAN_CREATED";
            case "executor" -> "AFTER_EACH_STEP_EXECUTED";
            case "evidence_validation" -> "AFTER_EVIDENCE_VALIDATION";
            case "diagnosis" -> "AFTER_DIAGNOSIS";
            case "risk_assessment" -> "AFTER_RISK_ASSESSMENT";
            case "final_response" -> "AFTER_FINAL_RESPONSE";
            default -> node.toUpperCase();
        };
    }

    private String summarize(Map<String, Object> output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        String text = output.toString();
        return text.length() > 500 ? text.substring(0, 500) + "..." : text;
    }
}
