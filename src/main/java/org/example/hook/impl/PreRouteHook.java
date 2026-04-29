package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

@Component
public class PreRouteHook implements AgentHook {

    @Override
    public String getName() { return "pre_route_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String input = context.getInput();
        if (input == null || input.trim().isEmpty()) {
            return HookResult.block("输入内容为空，无法进行意图识别");
        }

        if (input.length() > 2000) {
            context.setInput(input.substring(0, 2000));
        }

        String sanitized = input.replaceAll("<script.*?>.*?</script>", "[FILTERED]")
                .replaceAll("javascript:", "[FILTERED]")
                .replaceAll("ignore previous instructions", "[FILTERED]");
        context.setInput(sanitized);

        return HookResult.proceed();
    }
}
