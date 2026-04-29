package org.example.agent.subagent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubagentTask {
    private String taskId;
    private String subagentName;
    private String input;
    private Map<String, Object> context;
    private String result;
    private String status;
    private long durationMs;
}
