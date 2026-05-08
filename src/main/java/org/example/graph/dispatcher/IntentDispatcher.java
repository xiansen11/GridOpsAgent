package org.example.graph.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class IntentDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String intent = state.value("intent").map(Object::toString).orElse("CHAT");

        return switch (intent) {
            case "KNOWLEDGE_QA" -> "knowledge_qa";
            case "DIAGNOSIS" -> "diagnosis";
            default -> "chat";
        };
    }
}
