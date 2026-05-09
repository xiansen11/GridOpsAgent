package org.example.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HookEngine {

    private static final Logger logger = LoggerFactory.getLogger(HookEngine.class);

    private final Map<String, List<AgentHook>> hookRegistry = new ConcurrentHashMap<>();

    public void registerHook(String hookPoint, AgentHook hook) {
        hookRegistry.computeIfAbsent(hookPoint, k -> new ArrayList<>()).add(hook);
        hookRegistry.get(hookPoint).sort(Comparator.comparingInt(AgentHook::getOrder));
        logger.info("注册Hook: hookPoint={}, hookName={}, order={}", hookPoint, hook.getName(), hook.getOrder());
    }

    public HookResult executeHooks(String hookPoint, HookContext context) {
        List<AgentHook> hooks = hookRegistry.get(hookPoint);
        if (hooks == null || hooks.isEmpty()) {
            return HookResult.proceed();
        }

        context.setHookPoint(hookPoint);

        for (AgentHook hook : hooks) {
            if (!hook.isEnabled()) continue;

            try {
                logger.debug("执行Hook: hookPoint={}, hookName={}", hookPoint, hook.getName());
                HookResult result = hook.execute(context);

                if (!result.isProceed()) {
                    logger.info("Hook拦截: hookPoint={}, hookName={}, reason={}",
                            hookPoint, hook.getName(), result.getMessage());
                    return result;
                }
            } catch (Exception e) {
                logger.error("Hook执行异常: hookPoint={}, hookName={}", hookPoint, hook.getName(), e);
            }
        }

        return HookResult.proceed();
    }

    public List<String> getRegisteredHookPoints() {
        return new ArrayList<>(hookRegistry.keySet());
    }

    public List<Map<String, Object>> getHooksInfo(String hookPoint) {
        List<AgentHook> hooks = hookRegistry.get(hookPoint);
        if (hooks == null) return Collections.emptyList();

        List<Map<String, Object>> info = new ArrayList<>();
        for (AgentHook hook : hooks) {
            Map<String, Object> hookInfo = new LinkedHashMap<>();
            hookInfo.put("name", hook.getName());
            hookInfo.put("order", hook.getOrder());
            hookInfo.put("enabled", hook.isEnabled());
            info.add(hookInfo);
        }
        return info;
    }
}
