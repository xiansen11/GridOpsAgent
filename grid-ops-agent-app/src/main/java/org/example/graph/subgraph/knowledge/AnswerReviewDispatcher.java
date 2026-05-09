package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class AnswerReviewDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String decision = state.value("review_decision").map(Object::toString).orElse("ACCEPT");
        return switch (decision) {
            case "NEED_MORE" -> "rag_retrieve";
            default -> "citation_check";
        };
    }
}
