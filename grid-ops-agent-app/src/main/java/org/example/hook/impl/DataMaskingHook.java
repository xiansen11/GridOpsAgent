package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

@Component
public class DataMaskingHook implements AgentHook {

    @Override
    public String getName() { return "data_masking_hook"; }

    @Override
    public int getOrder() { return 30; }

    @Override
    public HookResult execute(HookContext context) {
        if (context.getOutput() == null) return HookResult.proceed();

        String output = context.getOutput();
        output = output.replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");
        output = output.replaceAll("(\\w{3})\\w+(@\\w+\\.\\w+)", "$1***$2");
        context.setOutput(output);
        return HookResult.proceed();
    }
}
