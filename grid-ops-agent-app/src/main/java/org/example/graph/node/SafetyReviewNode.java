package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.hook.HookContext;
import org.example.hook.HookEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SafetyReviewNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(SafetyReviewNode.class);
    private final HookEngine hookEngine;

    public SafetyReviewNode(HookEngine hookEngine) {
        this.hookEngine = hookEngine;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String response = state.value("final_response").map(Object::toString).orElse("");
        String sessionId = state.value("session_id").map(Object::toString).orElse("default");
        String taskId = state.value("task_id").map(Object::toString).orElse("unknown");

        logger.info("SafetyReviewNode: 安全审查");

        HookContext ctx = HookContext.builder()
                .sessionId(sessionId).taskId(taskId)
                .input("").output(response).params(new HashMap<>()).build();
        hookEngine.executeHooks("POST_DIAGNOSIS", ctx);

        String reviewed = ctx.getOutput();
        if (reviewed == null || reviewed.isEmpty()) {
            reviewed = response;
        }

        return Map.of("final_response", reviewed);
    }
}
