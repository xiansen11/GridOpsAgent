package org.example.graph.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StepHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(StepHandlerRegistry.class);
    private final Map<String, PlanStepHandler> handlers = new HashMap<>();

    public void register(PlanStepHandler handler) {
        handlers.put(handler.agentType(), handler);
        logger.info("注册 StepHandler: agentType={}", handler.agentType());
    }

    public PlanStepHandler getHandler(String agentType) {
        PlanStepHandler handler = handlers.get(agentType);
        if (handler == null) {
            logger.warn("未找到 agentType={} 的 Handler，使用 chat Handler", agentType);
            handler = handlers.get("chat");
        }
        return handler;
    }

    public Map<String, PlanStepHandler> getAllHandlers() {
        return Map.copyOf(handlers);
    }
}
