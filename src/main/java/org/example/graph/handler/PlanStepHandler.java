package org.example.graph.handler;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;

public interface PlanStepHandler {
    String agentType();
    StepResult execute(PlanStep step, OverAllState state);
}
