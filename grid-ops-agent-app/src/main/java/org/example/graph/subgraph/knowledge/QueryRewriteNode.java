package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.security.RbacService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

public class QueryRewriteNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(QueryRewriteNode.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RbacService rbacService;
    private final ChatClient chatClient;

    public QueryRewriteNode(RbacService rbacService, ChatClient chatClient) {
        this.rbacService = rbacService;
        this.chatClient = chatClient;
    }

    public QueryRewriteNode(ChatClient chatClient) {
        this.rbacService = null;
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String memoryContext = state.value("memory_context").map(Object::toString).orElse("");
        String userId = state.value("user_id").map(Object::toString).orElse("default");

        logger.info("QueryRewriteNode: LLM查询改写+实体提取+权限校验");

        if (rbacService != null) {
            boolean hasPermission = rbacService.hasPermission(userId, "DEVICE_STATUS");
            if (!hasPermission) {
                return Map.of("final_response", "您没有设备查询权限，请联系管理员。", "permission_granted", false);
            }
        }

        String rewritePrompt = """
                你是电力智能运维平台的查询理解专家。请对用户的问题进行以下处理：

                1. 查询改写：将口语化、含代词的查询改写为清晰完整的查询
                   - 代词消解：将"它"、"该设备"等替换为具体设备名称
                   - 补全隐含信息：如"油温高"补全为"变压器油温高"
                   - 保留原始查询的核心意图

                2. 实体提取：从查询中提取关键实体
                   - deviceId: 设备编号（如TR-110KV-001、SW-35KV-001、GIS-220KV-001等）
                   - deviceType: 设备类型（如变压器、开关柜、GIS设备、线路等）
                   - attribute: 查询属性（如oilTemp-油温、status-状态、alarmHistory-告警、profile-台账、safetyRules-安规）

                请严格按以下JSON格式输出，不要输出其他内容：
                {"rewritten_query": "改写后的查询", "entities": {"deviceId": "设备编号或UNKNOWN", "deviceType": "设备类型或未知", "attribute": "查询属性"}}
                """;

        String userMessage = "用户问题：" + input;
        if (!memoryContext.isEmpty()) {
            userMessage += "\n\n对话上下文（用于代词消解）：\n" + memoryContext;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("permission_granted", true);

        try {
            String llmResponse = chatClient.prompt()
                    .system(rewritePrompt)
                    .user(userMessage)
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
            String rewrittenQuery = (String) parsed.getOrDefault("rewritten_query", input);
            Object entitiesObj = parsed.get("entities");

            Map<String, String> entities = new HashMap<>();
            if (entitiesObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    entities.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }

            result.put("cleaned_input", rewrittenQuery);
            result.put("entities", entities);
            result.put("rewritten_query", rewrittenQuery);

            logger.info("QueryRewriteNode: LLM改写完成, original={}, rewritten={}, entities={}",
                    input.substring(0, Math.min(30, input.length())),
                    rewrittenQuery.substring(0, Math.min(30, rewrittenQuery.length())),
                    entities);
        } catch (Exception e) {
            logger.warn("QueryRewriteNode: LLM改写失败，使用原始输入, error={}", e.getMessage());
            result.put("cleaned_input", input);
            result.put("entities", Map.of("deviceId", "UNKNOWN", "deviceType", "未知", "attribute", "status"));
        }

        return result;
    }
}
