package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.example.security.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PreToolUseHook implements AgentHook {

    @Autowired
    private RbacService rbacService;

    private static final Map<String, Set<String>> TOOL_PERMISSIONS = Map.of(
            "getDeviceStatus", Set.of("QUERY"),
            "getAlarmHistory", Set.of("QUERY"),
            "getDeviceLogs", Set.of("QUERY"),
            "getDefectTickets", Set.of("QUERY"),
            "searchSafetyRules", Set.of("QUERY"),
            "getDeviceProfile", Set.of("QUERY"),
            "queryInternalDocs", Set.of("QUERY"),
            "createEmergencyTicket", Set.of("TICKET_CREATE"),
            "shutdownDevice", Set.of("DEVICE_CONTROL"),
            "reduceLoad", Set.of("LOAD_CONTROL")
    );

    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
            "createEmergencyTicket", "shutdownDevice", "reduceLoad"
    );

    @Override
    public String getName() { return "pre_tool_use_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String toolName = (String) context.getParam("toolName");
        String userId = (String) context.getParam("userId");

        if (toolName == null) {
            return HookResult.proceed();
        }

        Set<String> requiredPermissions = TOOL_PERMISSIONS.getOrDefault(toolName, Set.of("QUERY"));

        if (userId != null) {
            for (String permission : requiredPermissions) {
                if (!rbacService.hasPermission(userId, permission)) {
                    return HookResult.block("用户 " + userId + " 无权调用工具 " + toolName + "，缺少权限: " + permission);
                }
            }
        }

        if (HIGH_RISK_TOOLS.contains(toolName)) {
            context.setParam("requireHumanApproval", true);
            context.setParam("approvalReason", "工具 " + toolName + " 为高风险操作，需要人工确认");
        }

        return HookResult.proceed();
    }
}
