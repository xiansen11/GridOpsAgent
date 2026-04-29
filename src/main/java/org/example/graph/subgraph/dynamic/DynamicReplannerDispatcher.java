package org.example.graph.subgraph.dynamic;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class DynamicReplannerDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextAction = state.value("next_action").map(Object::toString).orElse("CONTINUE");
        return switch (nextAction) {
            case "REPLAN" -> "dynamic_planner";
            case "HUMAN_APPROVAL" -> "finalize_plan";
            case "END", "FALLBACK" -> "finalize_plan";
            default -> "dynamic_executor";
        };
    }
}
