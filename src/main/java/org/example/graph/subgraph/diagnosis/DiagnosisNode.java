package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.example.agent.diagnosis.DiagnosisAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DiagnosisNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosisNode.class);
    private final DiagnosisAgent diagnosisAgent;
    private final String dashScopeApiKey;

    public DiagnosisNode(DiagnosisAgent diagnosisAgent, String dashScopeApiKey) {
        this.diagnosisAgent = diagnosisAgent;
        this.dashScopeApiKey = dashScopeApiKey;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String evidence = state.value("execution_result").map(Object::toString).orElse("");
        String skillContext = state.value("skill_context").map(Object::toString).orElse("");

        logger.info("DiagnosisNode: 综合诊断");

        String diagnosisInput = input + "\n\n--- 收集的证据 ---\n" + evidence;
        if (!skillContext.isEmpty()) {
            diagnosisInput += "\n\n--- 业务场景指导 ---\n" + skillContext;
        }

        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        String result = diagnosisAgent.create(dashScopeApi).call(diagnosisInput).getText();

        return Map.of("execution_result", result);
    }
}
