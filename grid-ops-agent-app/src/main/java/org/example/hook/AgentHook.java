package org.example.hook;

import java.util.Map;

public interface AgentHook {
    String getName();
    HookResult execute(HookContext context);
    default int getOrder() { return 100; }
    default boolean isEnabled() { return true; }
}
