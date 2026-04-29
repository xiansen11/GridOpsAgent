package org.example.hook.impl;

import org.example.hook.AgentHook;
import org.example.hook.HookContext;
import org.example.hook.HookResult;
import org.springframework.stereotype.Component;

@Component
public class PreRagHook implements AgentHook {

    @Override
    public String getName() { return "pre_rag_hook"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public HookResult execute(HookContext context) {
        String query = context.getInput();
        if (query == null || query.trim().isEmpty()) {
            return HookResult.block("RAG检索查询为空");
        }

        String expanded = expandQuery(query);
        context.setInput(expanded);

        return HookResult.proceed();
    }

    private String expandQuery(String query) {
        if (query.contains("油温") && !query.contains("变压器")) {
            return "变压器 " + query;
        }
        if (query.contains("局放") && !query.contains("开关柜")) {
            return "开关柜 " + query;
        }
        if (query.contains("跳闸") && !query.contains("线路")) {
            return "配电线路 " + query;
        }
        return query;
    }
}
