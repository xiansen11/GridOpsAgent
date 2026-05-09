package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.memory.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MemorySaveNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(MemorySaveNode.class);
    private final MemoryService memoryService;

    public MemorySaveNode(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String sessionId = state.value("session_id").map(Object::toString).orElse("default");
        String taskId = state.value("task_id").map(Object::toString).orElse("unknown");
        String userId = state.value("user_id").map(Object::toString).orElse("default");
        String input = state.value("input").map(Object::toString).orElse("");
        String response = state.value("final_response").map(Object::toString).orElse("");

        logger.info("MemorySaveNode: 保存对话记忆, sessionId={}", sessionId);

        memoryService.saveToSession(sessionId, "last_user_message", input);
        memoryService.saveToSession(sessionId, "last_assistant_message", response);

        return Map.of();
    }
}
