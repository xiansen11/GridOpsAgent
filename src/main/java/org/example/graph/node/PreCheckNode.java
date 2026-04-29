package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PreCheckNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(PreCheckNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("input").map(Object::toString).orElse("");
        logger.info("PreCheckNode: 输入校验, 原始长度={}", input.length());

        String cleaned = input.trim();
        cleaned = cleaned.replaceAll("<[^>]*>", "");
        if (cleaned.length() > 2000) {
            cleaned = cleaned.substring(0, 2000);
        }

        String[] dangerousKeywords = {"DROP TABLE", "DELETE FROM", "INSERT INTO", "UPDATE ", "--", ";--",
                "UNION SELECT", "OR 1=1", "<script", "javascript:"};
        for (String keyword : dangerousKeywords) {
            if (cleaned.toUpperCase().contains(keyword.toUpperCase())) {
                logger.warn("检测到潜在危险输入: {}", keyword);
                cleaned = cleaned.replace(keyword, "[FILTERED]");
            }
        }

        return Map.of("cleaned_input", cleaned);
    }
}
