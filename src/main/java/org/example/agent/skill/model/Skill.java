package org.example.agent.skill.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    private String skillId;
    private String name;
    private String version;
    private String description;

    @Builder.Default
    private String category = "general";

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int priority = 50;

    @Builder.Default
    private List<String> dependencies = List.of();

    @Builder.Default
    private Map<String, Object> config = Map.of();

    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    private List<String> applicableScenarios;
    private List<String> recommendedTools;
    private List<String> diagnosisWorkflow;
    private Map<String, Object> outputSchema;
    private String promptTemplate;
    private List<Map<String, String>> examples;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
