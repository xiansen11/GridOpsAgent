package org.example.graph.subgraph.alarm;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class AlarmSuggestionNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AlarmSuggestionNode.class);
    private final ChatClient chatClient;

    public AlarmSuggestionNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String reasoning = state.value("execution_result").map(Object::toString).orElse("");
        logger.info("AlarmSuggestionNode: 生成告警建议");

        String prompt = String.format(
                "你是电力运维告警处理专家。根据以下告警分析结果，生成处理建议。\n\n" +
                "告警信息: %s\n\n分析结果: %s\n\n" +
                "请给出：1.处理建议 2.安全注意事项 3.是否需要人工介入",
                input, reasoning);

        String result = chatClient.prompt().user(prompt).call().content();
        return Map.of("final_response", result);
    }
}
