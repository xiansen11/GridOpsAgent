package org.example.graph.subgraph.device;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EntityExtractNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EntityExtractNode.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("EntityExtractNode: 实体提取, input={}", input);

        Map<String, String> entities = new HashMap<>();
        if (input.contains("TR-110KV") || input.contains("1号主变")) {
            entities.put("deviceId", "TR-110KV-001");
            entities.put("deviceType", "变压器");
        } else if (input.contains("SW-35KV") || input.contains("开关柜")) {
            entities.put("deviceId", "SW-35KV-001");
            entities.put("deviceType", "开关柜");
        } else {
            entities.put("deviceId", "UNKNOWN");
            entities.put("deviceType", "未知");
        }

        if (input.contains("油温") || input.contains("温度")) {
            entities.put("attribute", "oilTemp");
        } else if (input.contains("状态")) {
            entities.put("attribute", "status");
        } else if (input.contains("告警")) {
            entities.put("attribute", "alarmHistory");
        } else if (input.contains("台账") || input.contains("参数")) {
            entities.put("attribute", "profile");
        } else {
            entities.put("attribute", "status");
        }

        return Map.of("entities", entities);
    }
}
