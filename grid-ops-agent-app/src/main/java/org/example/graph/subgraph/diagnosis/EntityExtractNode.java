package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

public class EntityExtractNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EntityExtractNode.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;

    public EntityExtractNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("EntityExtractNode: LLM实体提取+告警解析");

        String extractPrompt = """
                你是电力智能运维平台的实体识别专家。请从告警或故障描述中提取关键实体信息。

                需要提取的实体：
                - deviceId: 设备编号（如TR-110KV-001、SW-35KV-001、GIS-220KV-001、LINE-10KV-001等）
                - deviceType: 设备类型（如变压器、开关柜、GIS设备、配电线路等）
                - faultType: 故障类型（如油温异常、局放超标、SF6压力低、跳闸、接地故障等）
                - alarmType: 告警类型（如油温高告警、局放超标告警、SF6压力低告警、跳闸告警等）
                - alarmLevel: 告警等级（紧急、重要、一般）
                - station: 变电站名称（如有）

                请严格按以下JSON格式输出，不要输出其他内容：
                {"deviceId": "设备编号或UNKNOWN", "deviceType": "设备类型或未知", "faultType": "故障类型或通用故障", "alarmType": "告警类型或通用告警", "alarmLevel": "紧急/重要/一般", "station": "变电站名称或UNKNOWN"}
                """;

        Map<String, String> entities = new HashMap<>();
        String alarmLevel = "一般";

        try {
            String llmResponse = chatClient.prompt()
                    .system(extractPrompt)
                    .user("告警/故障描述：" + input)
                    .call()
                    .content();

            String jsonStr = llmResponse;
            if (llmResponse.contains("{")) {
                jsonStr = llmResponse.substring(llmResponse.indexOf("{"));
                if (jsonStr.contains("}")) {
                    jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("}") + 1);
                }
            }

            Map<String, Object> parsed = objectMapper.readValue(jsonStr, new TypeReference<>() {});
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                entities.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            alarmLevel = entities.getOrDefault("alarmLevel", "一般");
            logger.info("EntityExtractNode: LLM实体提取完成, entities={}", entities);
        } catch (Exception e) {
            logger.warn("EntityExtractNode: LLM提取失败，使用降级规则, error={}", e.getMessage());
            entities.put("deviceId", "UNKNOWN");
            entities.put("deviceType", "未知");
            entities.put("faultType", "通用故障");
            entities.put("alarmType", "通用告警");
            entities.put("alarmLevel", "一般");
            entities.put("station", "UNKNOWN");
        }

        return Map.of("entities", entities, "alarm_level", alarmLevel);
    }
}
