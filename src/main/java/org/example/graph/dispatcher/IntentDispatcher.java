package org.example.graph.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class IntentDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String intent = state.value("intent").map(Object::toString).orElse("GENERAL_CHAT");

        return switch (intent) {
            case "SAFETY_QA" -> "knowledge_qa";
            case "DEVICE_STATUS", "DEVICE_PROFILE" -> "device_query";
            case "ALARM_QUERY" -> "alarm_analysis";
            case "FAULT_DIAGNOSIS", "ALARM_DIAGNOSIS" -> "fault_diagnosis";
            case "COMPLEX_TASK" -> "dynamic_plan";
            default -> "chat";
        };
    }
}
