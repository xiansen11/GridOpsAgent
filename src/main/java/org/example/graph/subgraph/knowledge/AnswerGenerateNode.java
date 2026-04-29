package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
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
        String skillContext = state.value("skill_context").map(Object::toString).orElse("");

        logger.info("AnswerGenerateNode: 生成回答");

        String graphContext = knowledgeGraphService.buildGraphContext(input);

        String prompt = String.format(
                "请根据以下参考资料回答用户问题。如果参考资料中没有相关信息，请说明。\n\n" +
                "参考资料：\n%s\n\n知识图谱扩展：\n%s\n\n用户问题：%s\n\n%s",
                ragContext, graphContext, input,
                !skillContext.isEmpty() ? "业务场景指导：" + skillContext : "");

        String answer = chatClient.prompt().user(prompt).call().content();
        return Map.of("final_response", answer);
    }
}
