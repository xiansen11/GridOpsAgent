package org.example.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisTask {
    private String taskId;
    private String sessionId;
    private String userId;
    private String input;
    private TaskPlan taskPlan;
    private String status;
}
