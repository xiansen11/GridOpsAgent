package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class DiagnosisReplannerDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextAction = state.value("next_action").map(Object::toString).orElse("CONTINUE");
        return switch (nextAction) {
            case "REPLAN" -> "evidence_parallel";
            case "HUMAN_APPROVAL" -> "action_recommend";
            case "FALLBACK" -> "action_recommend";
            case "END" -> "action_recommend";
            default -> "action_recommend";
        };
    }
}
