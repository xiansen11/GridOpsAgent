package org.example.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {
    private int step;
    private String action;
    private String agentType;
    private Map<String, Object> params;
    private String purpose;
    private String status;
    private String result;
}
