package org.example.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlan {
    private String planId;
    private String taskId;
    @Builder.Default
    private List<PlanStep> steps = new ArrayList<>();
    private String status;
}
