package org.example.tool;

import org.example.entity.ToolInfo;
import org.example.security.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ToolSearchService {

    @Autowired
    private ToolRegistryService toolRegistryService;

    @Autowired
    private RbacService rbacService;

    private static final Map<String, List<String>> INTENT_TOOL_MAPPING = Map.of(
            "SAFETY_QA", List.of("searchSafetyRules", "queryInternalDocs"),
            "DEVICE_STATUS", List.of("getDeviceStatus", "getDeviceProfile"),
            "ALARM_QUERY", List.of("getAlarmHistory", "getDeviceStatus"),
            "DEVICE_PROFILE", List.of("getDeviceProfile"),
            "LOG_ANALYSIS", List.of("getDeviceLogs", "getDeviceStatus"),
            "TICKET_QUERY", List.of("getDefectTickets"),
            "FAULT_DIAGNOSIS", List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs", "getDefectTickets", "searchSafetyRules", "getDeviceProfile"),
            "ALARM_DIAGNOSIS", List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs", "getDefectTickets", "searchSafetyRules"),
            "GENERAL_CHAT", List.of("queryInternalDocs", "getCurrentDateTime")
    );

    public List<ToolInfo> searchByIntent(String intent) {
        List<String> toolNames = INTENT_TOOL_MAPPING.getOrDefault(intent, List.of());
        if (toolNames.isEmpty()) {
            return toolRegistryService.getAllTools();
        }

        return toolRegistryService.getAllTools().stream()
                .filter(tool -> toolNames.contains(tool.getToolName()))
                .collect(Collectors.toList());
    }

    public List<ToolInfo> searchByIntentWithPermission(String intent, String userId) {
        List<ToolInfo> tools = searchByIntent(intent);
        if (userId == null) return tools;

        return tools.stream()
                .filter(tool -> {
                    String permLevel = tool.getPermissionLevel();
                    if (permLevel == null || "QUERY".equals(permLevel)) return true;
                    return rbacService.hasPermission(userId, permLevel);
                })
                .collect(Collectors.toList());
    }

    public List<ToolInfo> searchByKeyword(String keyword) {
        return toolRegistryService.searchByKeyword(keyword);
    }

    public List<ToolInfo> searchByTags(List<String> tags) {
        return toolRegistryService.searchByTags(tags);
    }
}
