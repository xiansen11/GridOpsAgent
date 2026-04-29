package org.example.graph;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

public class PowerOpsStateFactory implements KeyStrategyFactory {

    @Override
    public Map<String, KeyStrategy> apply() {
        Map<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("input", new ReplaceStrategy());
        strategies.put("cleaned_input", new ReplaceStrategy());
        strategies.put("session_id", new ReplaceStrategy());
        strategies.put("task_id", new ReplaceStrategy());
        strategies.put("user_id", new ReplaceStrategy());
        strategies.put("intent", new ReplaceStrategy());
        strategies.put("confidence", new ReplaceStrategy());
        strategies.put("memory_context", new ReplaceStrategy());
        strategies.put("skill_context", new ReplaceStrategy());
        strategies.put("history", new ReplaceStrategy());
        strategies.put("plan", new ReplaceStrategy());
        strategies.put("current_step_index", new ReplaceStrategy());
        strategies.put("step_results", new AppendStrategy());
        strategies.put("execution_result", new ReplaceStrategy());
        strategies.put("next_action", new ReplaceStrategy());
        strategies.put("final_response", new ReplaceStrategy());
        strategies.put("loop_count", new ReplaceStrategy());
        strategies.put("entities", new ReplaceStrategy());
        strategies.put("evidence", new ReplaceStrategy());
        strategies.put("risk_level", new ReplaceStrategy());
        return strategies;
    }
}
