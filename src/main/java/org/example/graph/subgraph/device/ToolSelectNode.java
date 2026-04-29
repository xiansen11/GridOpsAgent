package org.example.graph.subgraph.device;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ToolSelectNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ToolSelectNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Object entitiesObj = state.value("entities").orElse(Map.of());
        Map<?, ?> entities = (entitiesObj instanceof Map) ? (Map<?, ?>) entitiesObj : Map.of();
        Object attrObj = entities.get("attribute");
        String attribute = attrObj != null ? attrObj.toString() : "status";
        logger.info("ToolSelectNode: 工具选择, attribute={}", attribute);

        String toolName = switch (attribute) {
            case "oilTemp", "status" -> "getDeviceStatus";
            case "alarmHistory" -> "getAlarmHistory";
            case "profile" -> "getDeviceProfile";
            default -> "getDeviceStatus";
        };

        return Map.of("execution_result", toolName);
    }
}
