package org.example.plugin;

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
public class PluginInfo {
    private String pluginId;
    private String name;
    private String version;
    private String description;
    private String category;
    private boolean enabled;
    private LocalDateTime installedAt;
    private String entryClass;
    private List<String> dependencies;
    private Map<String, Object> configSchema;
    private Map<String, Object> config;
}
