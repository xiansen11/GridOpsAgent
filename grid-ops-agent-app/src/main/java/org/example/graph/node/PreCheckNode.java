package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.graph.GraphStateKeys;
import org.example.graph.validation.InputValidator;
import org.example.graph.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PreCheckNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(PreCheckNode.class);
    private final InputValidator inputValidator;

    public PreCheckNode(InputValidator inputValidator) {
        this.inputValidator = inputValidator;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String input = state.value(GraphStateKeys.INPUT).map(Object::toString).orElse("");
        logger.info("PreCheckNode: input validation, originalLength={}", input.length());

        InputValidator.CleanInput cleanInput = inputValidator.validateAndClean(input);
        ValidationResult validation = cleanInput.validationResult();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(GraphStateKeys.CLEANED_INPUT, cleanInput.cleanedInput());
        result.put(GraphStateKeys.TASK_ID, state.value(GraphStateKeys.TASK_ID).map(Object::toString)
                .orElse("TASK-" + UUID.randomUUID().toString().substring(0, 8)));
        result.put(GraphStateKeys.SESSION_ID, state.value(GraphStateKeys.SESSION_ID).map(Object::toString)
                .orElse("session-" + UUID.randomUUID().toString().substring(0, 8)));
        result.put(GraphStateKeys.TRACE_ID, state.value(GraphStateKeys.TRACE_ID).map(Object::toString)
                .orElse("TRACE-" + UUID.randomUUID().toString().substring(0, 12)));

        if (!validation.getWarnings().isEmpty()) {
            result.put(GraphStateKeys.VALIDATION_WARNINGS, validation.getWarnings());
        }
        if (!validation.isValid()) {
            result.put(GraphStateKeys.NEXT_ACTION, "STOP");
            result.put(GraphStateKeys.FINAL_RESPONSE, String.join("; ", validation.getErrors()));
        }
        return result;
    }
}
