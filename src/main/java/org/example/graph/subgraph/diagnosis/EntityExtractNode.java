package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EntityExtractNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EntityExtractNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("EntityExtractNode: 实体提取+告警解析");

        Map<String, String> entities = new HashMap<>();

        if (input.contains("TR-110KV") || input.contains("1号主变") || input.contains("变压器")) {
            entities.put("deviceId", "TR-110KV-001");
            entities.put("deviceType", "变压器");
        } else if (input.contains("SW-35KV") || input.contains("开关柜")) {
            entities.put("deviceId", "SW-35KV-001");
            entities.put("deviceType", "开关柜");
        } else if (input.contains("GIS") || input.contains("GIS设备")) {
            entities.put("deviceId", "GIS-220KV-001");
            entities.put("deviceType", "GIS设备");
        } else {
            entities.put("deviceId", "UNKNOWN");
            entities.put("deviceType", "未知");
        }

        if (input.contains("油温")) {
            entities.put("faultType", "油温异常");
            entities.put("alarmType", "油温高告警");
        } else if (input.contains("局放")) {
            entities.put("faultType", "局放超标");
            entities.put("alarmType", "局放超标告警");
        } else if (input.contains("SF6") || input.contains("压力")) {
            entities.put("faultType", "SF6压力低");
            entities.put("alarmType", "SF6压力低告警");
        } else {
            entities.put("faultType", "通用故障");
            entities.put("alarmType", "通用告警");
        }

        if (input.contains("紧急")) {
            entities.put("alarmLevel", "紧急");
        } else if (input.contains("重要")) {
            entities.put("alarmLevel", "重要");
        } else {
            entities.put("alarmLevel", "一般");
        }

        return Map.of("entities", entities, "alarm_level", entities.get("alarmLevel"));
    }
}
