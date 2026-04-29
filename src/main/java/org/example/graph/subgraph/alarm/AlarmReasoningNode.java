package org.example.graph.subgraph.alarm;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class AlarmReasoningNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AlarmReasoningNode.class);
    private final ChatClient chatClient;

    public AlarmReasoningNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String evidence = state.value("execution_result").map(Object::toString).orElse("");
        logger.info("AlarmReasoningNode: 告警推理");

        String prompt = String.format(
                "你是电力运维告警分析专家。根据以下告警信息和关联数据，分析告警原因和影响范围。\n\n" +
                "告警信息: %s\n\n关联数据: %s\n\n" +
                "请给出：1.告警原因分析 2.影响范围评估 3.紧急程度判断",
                input, evidence);

        String result = chatClient.prompt().user(prompt).call().content();
        return Map.of("execution_result", result);
    }
}
