package org.example.graph.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult {
    @JsonAlias("step_id")
    private String stepId;
    @JsonAlias("step_no")
    private Integer stepNo;
    private String action;
    @JsonAlias({"tool_name", "tool"})
    private String toolName;
    private String status;
    private boolean success;
    private String result;
    private String error;
    @JsonAlias("error_type")
    private String errorType;
    @JsonAlias("retry_count")
    private Integer retryCount;
    private Boolean recoverable;
    @JsonAlias("next_suggestion")
    private String nextSuggestion;
    @JsonAlias("match_expected")
    private Boolean matchExpected;
    @JsonAlias("duration_ms")
    private Long durationMs;
    @JsonAlias("evidence_type")
    private String evidenceType;
    private Map<String, Object> metadata;
}
