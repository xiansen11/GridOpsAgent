package org.example.graph.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.example.graph.GraphStateKeys;

public class EvidenceValidationDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextAction = state.value(GraphStateKeys.NEXT_ACTION).map(Object::toString).orElse("CONTINUE");
        return switch (nextAction) {
            case "REPLAN" -> "replanner";
            case "FALLBACK" -> "action_recommend";
            default -> "diagnosis";
        };
    }
}
