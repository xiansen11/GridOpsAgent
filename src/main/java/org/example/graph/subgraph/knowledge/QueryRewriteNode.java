package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.security.RbacService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class QueryRewriteNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(QueryRewriteNode.class);
    private final RbacService rbacService;

    public QueryRewriteNode(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    public QueryRewriteNode() {
        this.rbacService = null;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String memoryContext = state.value("memory_context").map(Object::toString).orElse("");
        String userId = state.value("user_id").map(Object::toString).orElse("default");

        logger.info("QueryRewriteNode: 查询改写+实体提取+权限校验");

        if (rbacService != null) {
            boolean hasPermission = rbacService.hasPermission(userId, "DEVICE_STATUS");
            if (!hasPermission) {
                return Map.of("final_response", "您没有设备查询权限，请联系管理员。", "permission_granted", false);
            }
        }

        String rewritten = input;
        if (input.contains("油温高") && !input.contains("变压器")) {
            rewritten = "变压器" + input;
        }
        if (input.contains("局放") && !input.contains("开关柜")) {
            rewritten = "开关柜" + input;
        }
        if ((input.contains("它") || input.contains("该设备")) && !memoryContext.isEmpty()) {
            if (memoryContext.contains("TR-110KV")) {
                rewritten = rewritten.replace("它", "TR-110KV-001主变").replace("该设备", "TR-110KV-001主变");
            }
        }

        Map<String, String> entities = new HashMap<>();
        if (rewritten.contains("TR-110KV") || rewritten.contains("1号主变") || rewritten.contains("变压器")) {
            entities.put("deviceId", "TR-110KV-001");
            entities.put("deviceType", "变压器");
        } else if (rewritten.contains("SW-35KV") || rewritten.contains("开关柜")) {
            entities.put("deviceId", "SW-35KV-001");
            entities.put("deviceType", "开关柜");
        } else if (rewritten.contains("GIS") || rewritten.contains("GIS设备")) {
            entities.put("deviceId", "GIS-220KV-001");
            entities.put("deviceType", "GIS设备");
        } else {
            entities.put("deviceId", "UNKNOWN");
            entities.put("deviceType", "未知");
        }

        if (rewritten.contains("油温") || rewritten.contains("温度")) {
            entities.put("attribute", "oilTemp");
        } else if (rewritten.contains("状态") || rewritten.contains("负荷")) {
            entities.put("attribute", "status");
        } else if (rewritten.contains("告警")) {
            entities.put("attribute", "alarmHistory");
        } else if (rewritten.contains("台账") || rewritten.contains("参数") || rewritten.contains("型号")) {
            entities.put("attribute", "profile");
        } else if (rewritten.contains("安规") || rewritten.contains("规程") || rewritten.contains("安全")) {
            entities.put("attribute", "safetyRules");
        } else {
            entities.put("attribute", "status");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cleaned_input", rewritten);
        result.put("entities", entities);
        result.put("permission_granted", true);
        return result;
    }
}
