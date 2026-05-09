package org.example.graph.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {
    @JsonAlias("step_id")
    private String stepId;
    @JsonAlias("step_no")
    private Integer stepNo;
    @JsonAlias("step_type")
    private String stepType;
    private int step;
    private String action;
    @JsonAlias("agent_type")
    private String agentType;
    @JsonAlias({"tool_name", "tool"})
    private String toolName;
    private String tool;
    private Map<String, Object> params;
    private String purpose;
    private String expected;
    @JsonAlias("depends_on")
    private List<String> dependsOn;
    private String status;
    @JsonAlias("retry_count")
    private Integer retryCount;
    private Boolean required;
    private String result;

    public String effectiveStepId() {
        if (stepId != null && !stepId.isBlank()) {
            return stepId;
        }
        int no = effectiveStepNo();
        return String.format("step-%03d", Math.max(no, 1));
    }

    public int effectiveStepNo() {
        if (stepNo != null && stepNo > 0) {
            return stepNo;
        }
        return step > 0 ? step : 1;
    }

    public String effectiveStepType() {
        return stepType == null || stepType.isBlank() ? "TOOL_CALL" : stepType;
    }

    public String effectiveToolName() {
        if (toolName != null && !toolName.isBlank()) {
            return toolName;
        }
        if (tool != null && !tool.isBlank()) {
            return tool;
        }
        return agentType;
    }

    public String effectiveStatus() {
        return status == null || status.isBlank() ? "PENDING" : status;
    }

    public int effectiveRetryCount() {
        return retryCount == null ? 0 : retryCount;
    }

    public boolean effectiveRequired() {
        return required == null || required;
    }

    public List<String> effectiveDependsOn() {
        return dependsOn == null ? new ArrayList<>() : dependsOn;
    }
}
