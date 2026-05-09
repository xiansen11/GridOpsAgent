package org.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.agent.skill.model.Skill;
import org.example.agent.skill.service.SkillSelector;
import org.example.memory.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ContextLoadNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ContextLoadNode.class);

    private final MemoryService memoryService;
    private final SkillSelector skillSelector;

    public ContextLoadNode(MemoryService memoryService, SkillSelector skillSelector) {
        this.memoryService = memoryService;
        this.skillSelector = skillSelector;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String sessionId = state.value("session_id").map(Object::toString).orElse("default");
        String taskId = state.value("task_id").map(Object::toString).orElse("TASK-" + System.currentTimeMillis());
        String userId = state.value("user_id").map(Object::toString).orElse("default");
        String intent = state.value("intent").map(Object::toString).orElse("");

        logger.info("ContextLoadNode: 加载上下文, sessionId={}, intent={}", sessionId, intent);

        String memoryContext = memoryService.buildContextForAgent(sessionId, taskId, userId);

        String skillContext = "";
        if (!intent.isEmpty()) {
            Skill skill = skillSelector.selectByIntent(intent).orElse(null);
            if (skill != null && skill.getPromptTemplate() != null) {
                skillContext = skill.getPromptTemplate();
            }
        }

        return Map.of(
                "memory_context", memoryContext,
                "skill_context", skillContext
        );
    }
}
