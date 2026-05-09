package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.agent.diagnosis.DiagnosisAgent;
import org.example.graph.GraphStateKeys;
import org.example.graph.validation.DiagnosisValidator;
import org.example.graph.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiagnosisNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosisNode.class);

    private final DiagnosisAgent diagnosisAgent;
    private final String dashScopeApiKey;
    private final DiagnosisValidator diagnosisValidator;

    public DiagnosisNode(DiagnosisAgent diagnosisAgent, String dashScopeApiKey,
                         DiagnosisValidator diagnosisValidator) {
        this.diagnosisAgent = diagnosisAgent;
        this.dashScopeApiKey = dashScopeApiKey;
        this.diagnosisValidator = diagnosisValidator;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value(GraphStateKeys.CLEANED_INPUT).map(Object::toString).orElse("");
        String evidence = state.value(GraphStateKeys.EVIDENCE).map(Object::toString)
                .orElse(state.value(GraphStateKeys.EXECUTION_RESULT).map(Object::toString).orElse(""));
        String skillContext = state.value("skill_context").map(Object::toString).orElse("");
        Object evidenceScore = state.value(GraphStateKeys.EVIDENCE_SCORE).orElse(null);
        Object evidenceCoverage = state.value(GraphStateKeys.EVIDENCE_COVERAGE).orElse(null);

        logger.info("DiagnosisNode: generating diagnosis, evidenceScore={}", evidenceScore);

        StringBuilder diagnosisInput = new StringBuilder();
        diagnosisInput.append(input).append("\n\n--- Evidence ---\n").append(evidence);
        if (evidenceScore != null) {
            diagnosisInput.append("\n\nEvidence score: ").append(evidenceScore);
        }
        if (evidenceCoverage != null) {
            diagnosisInput.append("\nEvidence coverage: ").append(evidenceCoverage);
        }
        if (!skillContext.isBlank()) {
            diagnosisInput.append("\n\n--- Business Context ---\n").append(skillContext);
        }
        diagnosisInput.append("""

                Please produce a structured diagnosis report that includes:
                - Alarm summary
                - Key evidence
                - Possible causes
                - Risk level
                - Handling suggestions
                - Safety notes
                - Uncertainty statement
                """);

        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        String result = diagnosisAgent.create(dashScopeApi).call(diagnosisInput.toString()).getText();
        ValidationResult validation = diagnosisValidator.validate(result);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(GraphStateKeys.DIAGNOSIS_RESULT, result);
        output.put(GraphStateKeys.EXECUTION_RESULT, result);
        if (!validation.isValid()) {
            output.put(GraphStateKeys.VALIDATION_WARNINGS, validation.getWarnings());
        }
        return output;
    }
}
