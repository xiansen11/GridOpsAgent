package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

public class AnswerReviewNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AnswerReviewNode.class);
    private static final int MAX_REVIEW_LOOP = 2;
    private final ChatClient chatClient;

    public AnswerReviewNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String response = state.value("final_response").map(Object::toString).orElse("");
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String ragContext = state.value("execution_result").map(Object::toString).orElse("");
        int reviewLoop = state.value("review_loop").map(v -> Integer.parseInt(v.toString())).orElse(0);

        logger.info("AnswerReviewNode: 回答质量评估, reviewLoop={}", reviewLoop);

        if (reviewLoop >= MAX_REVIEW_LOOP) {
            logger.info("AnswerReviewNode: 达到最大评估循环次数，接受当前回答");
            return Map.of("review_decision", "ACCEPT");
        }

        if (response.isEmpty() || response.length() < 20) {
            logger.info("AnswerReviewNode: 回答过短，需要补充检索");
            return Map.of("review_decision", "NEED_MORE", "review_loop", reviewLoop + 1);
        }

        String reviewPrompt = """
                你是回答质量评估专家。请评估以下回答是否充分回答了用户的问题。

                评估标准：
                1. 回答是否直接回应了用户的问题
                2. 回答是否提供了具体、有用的信息
                3. 回答是否避免了"无法回答"或"信息不足"等模糊表述

                请严格按以下JSON格式输出：
                {"decision": "ACCEPT或NEED_MORE", "reason": "评估理由"}

                - ACCEPT: 回答充分，质量合格
                - NEED_MORE: 回答不够充分，需要补充检索
                """;

        String userMessage = "用户问题：" + input + "\n\n当前回答：" + response;

        try {
            String llmResponse = chatClient.prompt()
                    .system(reviewPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            String decision = "ACCEPT";
            if (llmResponse.contains("NEED_MORE")) {
                decision = "NEED_MORE";
            }

            logger.info("AnswerReviewNode: 评估结果={}, reviewLoop={}", decision, reviewLoop);
            return Map.of("review_decision", decision, "review_loop", reviewLoop + 1);
        } catch (Exception e) {
            logger.warn("AnswerReviewNode: LLM评估失败，接受当前回答, error={}", e.getMessage());
            return Map.of("review_decision", "ACCEPT");
        }
    }
}
