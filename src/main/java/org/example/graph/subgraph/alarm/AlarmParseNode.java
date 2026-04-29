package org.example.graph.subgraph.alarm;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AlarmParseNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AlarmParseNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("AlarmParseNode: 告警解析");

        Map<String, String> alarmInfo = new HashMap<>();
        if (input.contains("油温")) {
            alarmInfo.put("alarmType", "油温高告警");
            alarmInfo.put("deviceType", "变压器");
        } else if (input.contains("局放")) {
            alarmInfo.put("alarmType", "局放超标告警");
            alarmInfo.put("deviceType", "开关柜");
        } else if (input.contains("SF6") || input.contains("压力")) {
            alarmInfo.put("alarmType", "SF6压力低告警");
            alarmInfo.put("deviceType", "GIS设备");
        } else {
            alarmInfo.put("alarmType", "通用告警");
            alarmInfo.put("deviceType", "未知");
        }

        if (input.contains("紧急")) alarmInfo.put("alarmLevel", "紧急");
        else if (input.contains("重要")) alarmInfo.put("alarmLevel", "重要");
        else alarmInfo.put("alarmLevel", "一般");

        return Map.of("entities", alarmInfo);
    }
}
