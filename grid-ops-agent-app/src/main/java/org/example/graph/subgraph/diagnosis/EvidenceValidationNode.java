package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.graph.GraphStateKeys;
import org.example.graph.model.StepResult;
import org.example.graph.validation.EvidenceQualityEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvidenceValidationNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceValidationNode.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EvidenceQualityEvaluator evidenceQualityEvaluator;

    public EvidenceValidationNode(EvidenceQualityEvaluator evidenceQualityEvaluator) {
        this.evidenceQualityEvaluator = evidenceQualityEvaluator;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String evidence = state.value(GraphStateKeys.EVIDENCE).map(Object::toString).orElse("");
        String previousNextAction = state.value(GraphStateKeys.NEXT_ACTION).map(Object::toString).orElse("");
        List<StepResult> stepResults = readStepResults(state.value(GraphStateKeys.STEP_RESULTS).orElse(List.of()));
        EvidenceQualityEvaluator.EvidenceScore score = evidenceQualityEvaluator.evaluate(stepResults, evidence);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(GraphStateKeys.EVIDENCE_SCORE, score.score());
        result.put(GraphStateKeys.EVIDENCE_COVERAGE, score.coverage());
        result.put(GraphStateKeys.EVIDENCE_WARNINGS, score.warnings());
        String nextAction = "REPLAN".equals(previousNextAction)
                ? "REPLAN"
                : "NEED_MORE".equals(score.decision()) ? "REPLAN" :
                "INSUFFICIENT".equals(score.decision()) ? "FALLBACK" : "CONTINUE";
        result.put(GraphStateKeys.NEXT_ACTION, nextAction);

        logger.info("EvidenceValidationNode: score={}, decision={}, warnings={}",
                score.score(), score.decision(), score.warnings().size());
        return result;
    }

    private List<StepResult> readStepResults(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return OBJECT_MAPPER.convertValue(list, new TypeReference<>() {});
    }
}
