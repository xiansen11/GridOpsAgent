package org.example.graph.subgraph.dynamic;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.graph.model.PlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

public class PlannerNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);
    private static final int MAX_RETRIES = 3;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PLANNER_PROMPT = """
            你是电力运维智能编排器。根据用户问题生成执行计划。
            可用Agent类型：tool(工具调用)、knowledge(知识问答)、alarm(告警分析)、
            diagnosis(综合诊断)、subagents(并行子Agent)、rag(RAG检索)、approval(审批)、chat(通用对话)。
            规则：1.简单任务1-2步；2.复杂任务3-5步；3.必须返回合法JSON。
            用户问题: %s
            意图: %s
            请返回JSON: {"steps": [{"step": 1, "action": "动作名", "agentType": "类型", "params": {}, "purpose": "目的"}]}
            """;

    public PlannerNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String intent = state.value("intent").map(Object::toString).orElse("COMPLEX_TASK");
        logger.info("PlannerNode: 动态计划生成");

        String prompt = String.format(PLANNER_PROMPT, input, intent);
        String planJson = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String response = chatClient.prompt().user(prompt).call().content();
                String cleaned = extractJson(response);
                Map<String, List<PlanStep>> parsed = objectMapper.readValue(cleaned,
                        new TypeReference<Map<String, List<PlanStep>>>() {});
                if (parsed.get("steps") != null && !parsed.get("steps").isEmpty()) {
                    planJson = cleaned;
                    break;
                }
            } catch (Exception e) {
                logger.warn("计划生成第{}次尝试失败: {}", attempt, e.getMessage());
            }
        }

        if (planJson == null) {
            planJson = "{\"steps\":[{\"step\":1,\"action\":\"chat\",\"agentType\":\"chat\",\"params\":{},\"purpose\":\"回答用户问题\"}]}";
        }

        return Map.of("plan", planJson, "current_step_index", 0, "loop_count", 0);
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }
}
