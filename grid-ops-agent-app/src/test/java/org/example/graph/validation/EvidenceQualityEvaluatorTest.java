package org.example.graph.validation;

import org.example.graph.model.StepResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceQualityEvaluatorTest {

    private final EvidenceQualityEvaluator evaluator = new EvidenceQualityEvaluator();

    @Test
    void scoresCoveredEvidence() {
        EvidenceQualityEvaluator.EvidenceScore score = evaluator.evaluate(List.of(
                StepResult.builder().success(true).evidenceType("DEVICE_STATUS").matchExpected(true).build(),
                StepResult.builder().success(true).evidenceType("ALARM_HISTORY").matchExpected(true).build(),
                StepResult.builder().success(true).evidenceType("DEVICE_LOGS").matchExpected(true).build(),
                StepResult.builder().success(true).evidenceType("SAFETY_RULES").matchExpected(true).build()
        ), "");

        assertThat(score.score()).isGreaterThanOrEqualTo(70);
        assertThat(score.decision()).isEqualTo("SUFFICIENT");
    }

    @Test
    void flagsThinEvidence() {
        EvidenceQualityEvaluator.EvidenceScore score = evaluator.evaluate(List.of(
                StepResult.builder().success(true).evidenceType("DEVICE_STATUS").matchExpected(true).build()
        ), "");

        assertThat(score.score()).isLessThan(40);
        assertThat(score.decision()).isEqualTo("INSUFFICIENT");
    }
}
