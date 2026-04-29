package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.agent.subagent.SubagentExecutor;
import org.example.agent.subagent.SubagentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvidenceParallelNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceParallelNode.class);
    private final SubagentExecutor subagentExecutor;

    public EvidenceParallelNode(SubagentExecutor subagentExecutor) {
        this.subagentExecutor = subagentExecutor;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("EvidenceParallelNode: 并行证据收集");

        List<SubagentTask> tasks = List.of(
                SubagentTask.builder().subagentName("regulation").input(input).build(),
                SubagentTask.builder().subagentName("metrics").input(input).build(),
                SubagentTask.builder().subagentName("log").input(input).build(),
                SubagentTask.builder().subagentName("ticket").input(input).build()
        );

        List<SubagentTask> results = subagentExecutor.executeParallel(tasks);

        Map<String, String> evidence = new HashMap<>();
        for (SubagentTask task : results) {
            evidence.put(task.getSubagentName(), task.getResult() != null ? task.getResult() : "分析失败");
        }

        StringBuilder aggregated = new StringBuilder();
        for (Map.Entry<String, String> entry : evidence.entrySet()) {
            aggregated.append("## ").append(entry.getKey()).append(" 分析结果\n");
            aggregated.append(entry.getValue()).append("\n\n");
        }

        return Map.of("evidence", evidence, "execution_result", aggregated.toString());
    }
}
