package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.rag.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class AnswerGenerateNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerateNode.class);
    private final ChatClient chatClient;
    private final KnowledgeGraphService knowledgeGraphService;

    public AnswerGenerateNode(ChatClient chatClient, KnowledgeGraphService knowledgeGraphService) {
        this.chatClient = chatClient;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String ragContext = state.value("execution_result").map(Object::toString).orElse("");
        String toolResult = state.value("tool_result").map(Object::toString).orElse("");
        String skillContext = state.value("skill_context").map(Object::toString).orElse("");

        logger.info("AnswerGenerateNode: 生成回答");

        String graphContext = "";
        if (knowledgeGraphService != null) {
            graphContext = knowledgeGraphService.buildGraphContext(input);
        }

        StringBuilder contextBuilder = new StringBuilder();
        if (!ragContext.isEmpty()) {
            contextBuilder.append("RAG检索结果：\n").append(ragContext).append("\n\n");
        }
        if (!toolResult.isEmpty()) {
            contextBuilder.append("工具查询结果：\n").append(toolResult).append("\n\n");
        }
        if (!graphContext.isEmpty()) {
            contextBuilder.append("知识图谱扩展：\n").append(graphContext).append("\n\n");
        }

        String prompt = String.format(
                "请根据以下参考资料回答用户问题。如果参考资料中没有相关信息，请说明。\n\n" +
                "%s用户问题：%s\n\n%s",
                contextBuilder, input,
                !skillContext.isEmpty() ? "业务场景指导：" + skillContext : "");

        String answer = chatClient.prompt().user(prompt).call().content();

        if (!toolResult.isEmpty() && !answer.contains("查询失败") && !answer.contains("执行失败")) {
            answer += "\n\n📌 以上数据来自设备监控系统，如需进一步分析请告知。";
        }

        return Map.of("final_response", answer);
    }
}
