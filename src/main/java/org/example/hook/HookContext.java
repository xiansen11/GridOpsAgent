package org.example.hook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HookContext {
    private String sessionId;
    private String taskId;
    private String agentName;
    private String hookPoint;
    private Map<String, Object> params;
    private String input;
    private String output;

    public Object getParam(String key) {
        return params != null ? params.get(key) : null;
    }

    public void setParam(String key, Object value) {
        if (params != null) params.put(key, value);
    }
}
