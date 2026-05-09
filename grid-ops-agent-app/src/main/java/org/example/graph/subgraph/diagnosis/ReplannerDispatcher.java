package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class ReplannerDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextAction = state.value("next_action").map(Object::toString).orElse("CONTINUE");
        return switch (nextAction) {
            case "REPLAN" -> "executor";
            case "HUMAN_APPROVAL", "FALLBACK", "END" -> "action_recommend";
            default -> "action_recommend";
        };
    }
}
