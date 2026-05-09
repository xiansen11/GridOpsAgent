package org.example.graph.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.graph.model.PlanStep;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PlanValidator {

    private static final int MIN_STEPS = 1;
    private static final int MAX_STEPS = 8;
    private static final Set<String> VALID_STEP_TYPES = Set.of("TOOL_CALL", "RAG_QUERY", "ANALYSIS", "APPROVAL", "FINALIZE");
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "RUNNING", "COMPLETED", "FAILED", "SKIPPED", "RETRYING");
    private static final Map<String, String> TOOL_ALIASES = Map.of(
            "queryDeviceStatus", "getDeviceStatus",
            "getDeviceTicket", "getDefectTickets",
            "getDeviceTickets", "getDefectTickets",
            "searchInternalDocs", "queryInternalDocs"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultPlanTemplateFactory defaultPlanTemplateFactory;

    public PlanValidator(DefaultPlanTemplateFactory defaultPlanTemplateFactory) {
        this.defaultPlanTemplateFactory = defaultPlanTemplateFactory;
    }

    public List<PlanStep> normalizeOrDefault(List<?> rawSteps, String input, Map<String, String> entities,
                                             ToolCallbackProvider tools, List<String> warnings) {
        List<PlanStep> steps = convert(rawSteps);
        if (steps.isEmpty()) {
            warnings.add("计划为空，已使用默认诊断模板");
            return defaultPlanTemplateFactory.createDefaultPlan(input, entities);
        }

        Set<String> availableTools = availableToolNames(tools);
        List<PlanStep> normalized = new ArrayList<>();
        int limit = Math.min(steps.size(), MAX_STEPS);
        if (steps.size() > MAX_STEPS) {
            warnings.add("计划步骤超过 " + MAX_STEPS + " 步，已截断");
        }

        for (int i = 0; i < limit; i++) {
            PlanStep step = steps.get(i);
            normalizeStep(step, i + 1, availableTools, warnings);
            normalized.add(step);
        }

        if (normalized.size() < MIN_STEPS) {
            warnings.add("计划步骤不足，已使用默认诊断模板");
            return defaultPlanTemplateFactory.createDefaultPlan(input, entities);
        }

        boolean hasInvalidRequiredTool = normalized.stream()
                .filter(step -> "TOOL_CALL".equals(step.effectiveStepType()))
                .anyMatch(step -> step.effectiveRequired() && !availableTools.isEmpty()
                        && !availableTools.contains(step.effectiveToolName()));
        if (hasInvalidRequiredTool) {
            warnings.add("计划包含无法修复的必需工具，已使用默认诊断模板");
            return defaultPlanTemplateFactory.createDefaultPlan(input, entities);
        }

        return normalized;
    }

    public ValidationResult validate(List<PlanStep> steps, ToolCallbackProvider tools) {
        ValidationResult result = ValidationResult.ok();
        if (steps == null || steps.isEmpty()) {
            return ValidationResult.invalid("plan_steps 不能为空", "EMPTY_PLAN", true);
        }
        Set<String> availableTools = availableToolNames(tools);
        for (PlanStep step : steps) {
            if (step.getAction() == null || step.getAction().isBlank()) {
                result.getErrors().add(step.effectiveStepId() + " 缺少 action");
            }
            if (!VALID_STEP_TYPES.contains(step.effectiveStepType())) {
                result.getErrors().add(step.effectiveStepId() + " step_type 非法: " + step.effectiveStepType());
            }
            if (!VALID_STATUSES.contains(step.effectiveStatus())) {
                result.getWarnings().add(step.effectiveStepId() + " status 非法，建议修正为 PENDING");
            }
            if ("TOOL_CALL".equals(step.effectiveStepType())) {
                String toolName = step.effectiveToolName();
                if (toolName == null || toolName.isBlank()) {
                    result.getErrors().add(step.effectiveStepId() + " 缺少 tool_name");
                } else if (!availableTools.isEmpty() && !availableTools.contains(toolName)) {
                    result.getErrors().add(step.effectiveStepId() + " 工具不存在: " + toolName);
                }
            }
        }
        result.setValid(result.getErrors().isEmpty());
        result.setRecoverable(!result.isValid());
        result.setErrorType(result.isValid() ? null : "INVALID_PLAN");
        return result;
    }

    private void normalizeStep(PlanStep step, int index, Set<String> availableTools, List<String> warnings) {
        step.setStepNo(step.effectiveStepNo() > 0 ? step.effectiveStepNo() : index);
        if (step.getStep() <= 0) {
            step.setStep(step.getStepNo());
        }
        if (step.getStepId() == null || step.getStepId().isBlank()) {
            step.setStepId(String.format("step-%03d", step.getStepNo()));
        }
        if (step.getStepType() == null || step.getStepType().isBlank() || !VALID_STEP_TYPES.contains(step.getStepType())) {
            step.setStepType("TOOL_CALL");
        }
        String toolName = step.effectiveToolName();
        if (TOOL_ALIASES.containsKey(toolName)) {
            toolName = TOOL_ALIASES.get(toolName);
            warnings.add(step.getStepId() + " 工具名已映射为 " + toolName);
        }
        if (toolName != null && !toolName.isBlank()) {
            step.setToolName(toolName);
            step.setTool(toolName);
        }
        if (step.getStatus() == null || !VALID_STATUSES.contains(step.getStatus())) {
            step.setStatus("PENDING");
        }
        if (step.getRetryCount() == null) {
            step.setRetryCount(0);
        }
        if (step.getRequired() == null) {
            step.setRequired(true);
        }
        if (step.getDependsOn() == null) {
            step.setDependsOn(List.of());
        }
        if (step.getParams() == null) {
            step.setParams(new LinkedHashMap<>());
        }
        if ("TOOL_CALL".equals(step.getStepType()) && !availableTools.isEmpty()
                && !availableTools.contains(step.effectiveToolName())) {
            warnings.add(step.getStepId() + " 工具不存在: " + step.effectiveToolName());
        }
    }

    private List<PlanStep> convert(List<?> rawSteps) {
        if (rawSteps == null) {
            return List.of();
        }
        return rawSteps.stream()
                .map(item -> objectMapper.convertValue(item, PlanStep.class))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Set<String> availableToolNames(ToolCallbackProvider tools) {
        if (tools == null) {
            return Set.of();
        }
        return Arrays.stream(tools.getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(definition -> definition.name())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
