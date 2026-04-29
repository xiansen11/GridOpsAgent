package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.example.observability.ObservabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PostToolUseHook implements AgentHook {

    @Autowired
    private ObservabilityService observabilityService;

    private static final String[] SENSITIVE_PATTERNS = {
            "密码", "password", "手机号", "身份证", "银行卡"
    };

    @Override
    public String getName() { return "post_tool_use_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String output = context.getOutput();
        String toolName = (String) context.getParam("toolName");

        if (output != null) {
            String masked = maskSensitiveData(output);
            context.setOutput(masked);
        }

        if (toolName != null) {
            try {
                observabilityService.logToolCall(
                        observabilityService.generateTraceId(),
                        context.getTaskId(),
                        context.getSessionId(),
                        toolName,
                        String.valueOf(context.getParam("toolInput")),
                        output != null ? (output.length() > 500 ? output.substring(0, 500) : output) : null,
                        "SUCCESS",
                        context.getParam("toolDuration") != null ? ((Number) context.getParam("toolDuration")).longValue() : 0L
                );
            } catch (Exception e) {
                // ignore observability errors
            }
        }

        return HookResult.proceed();
    }

    private String maskSensitiveData(String text) {
        String result = text;
        result = result.replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");
        result = result.replaceAll("(\\w+://[^:]+:)([^@]+)(@)", "$1****$3");
        return result;
    }
}
