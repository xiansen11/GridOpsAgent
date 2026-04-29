package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class ActionRecommendNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ActionRecommendNode.class);
    private final ChatClient chatClient;

    public ActionRecommendNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String diagnosis = state.value("execution_result").map(Object::toString).orElse("");
        String riskLevel = state.value("risk_level").map(Object::toString).orElse("MEDIUM");
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("ActionRecommendNode: 生成行动建议, riskLevel={}", riskLevel);

        String prompt = String.format(
                "你是电力运维专家。根据以下诊断结果，生成行动建议和安全提示。\n\n" +
                "原始问题: %s\n\n诊断结果: %s\n\n风险等级: %s\n\n" +
                "请给出：1.处理建议 2.安全注意事项 3.是否需要人工介入",
                input, diagnosis, riskLevel);

        String result = chatClient.prompt().user(prompt).call().content();

        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            result += "\n\n⚠️ **高风险操作提醒**：本诊断建议涉及高风险操作，请务必由专业人员确认后再执行。";
        }

        return Map.of("final_response", result);
    }
}
