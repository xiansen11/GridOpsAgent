package org.example.eval;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.graph.GraphStateKeys;
import org.example.graph.model.StepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EvalService {

    @Autowired
    private EvalDataset evalDataset;

    @Autowired
    private CompiledGraph compiledGraph;

    public List<EvalResult> runAllEvals() {
        return evalDataset.getAllCases().stream().map(this::runSingleEval).toList();
    }

    @SuppressWarnings("unchecked")
    public EvalResult runSingleEval(EvalCase evalCase) {
        long startTime = System.currentTimeMillis();
        EvalResult result = EvalResult.builder()
                .caseId(evalCase.getCaseId())
                .category(evalCase.getCategory())
                .question(evalCase.getQuestion())
                .build();

        try {
            Map<String, Object> initialState = new LinkedHashMap<>();
            initialState.put(GraphStateKeys.INPUT, evalCase.getQuestion());
            initialState.put(GraphStateKeys.SESSION_ID, "eval-session-" + evalCase.getCaseId());
            initialState.put(GraphStateKeys.USER_ID, "eval");
            initialState.put(GraphStateKeys.TASK_ID, "EVAL-" + evalCase.getCaseId());
            initialState.put(GraphStateKeys.TRACE_ID, "TRACE-EVAL-" + evalCase.getCaseId());

            Optional<OverAllState> resultOpt = compiledGraph.invoke(initialState);
            OverAllState finalState = resultOpt.orElse(null);
            String answer = finalState == null ? "" :
                    finalState.value(GraphStateKeys.FINAL_RESPONSE).map(Object::toString).orElse("");

            long duration = System.currentTimeMillis() - startTime;
            result.setActualAnswer(answer);
            result.setResponseTimeMs(duration);
            result.setResponseTimeOk(duration <= evalCase.getMaxResponseTimeMs());

            List<StepResult> stepResults = readStepResults(finalState);
            List<String> actualTools = stepResults.stream()
                    .map(StepResult::getToolName)
                    .filter(tool -> tool != null && !tool.isBlank())
                    .distinct()
                    .toList();
            result.setActualTools(actualTools);
            result.setToolSelectionAccuracy(scoreListMatch(evalCase.getExpectedTools(), actualTools));
            result.setToolCallSuccessRate(toolSuccessRate(stepResults));

            Map<String, Boolean> evidenceCoverage = finalState == null ? Map.of() :
                    (Map<String, Boolean>) finalState.value(GraphStateKeys.EVIDENCE_COVERAGE).orElse(Map.of());
            result.setEvidenceCoverageRate(evidenceCoverageRate(evalCase.getRequiredEvidenceTypes(), evidenceCoverage));
            result.setReplanCount(countNodeLoops(finalState, "replanner"));
            result.setFallbackTriggered(answer.contains("降级") || answer.contains("fallback") ||
                    "FALLBACK".equals(finalState == null ? null : finalState.value(GraphStateKeys.NEXT_ACTION).orElse(null)));
            result.setHumanApprovalTriggered(answer.contains("人工审批") || answer.contains("高风险"));

            String intent = finalState == null ? "" : finalState.value(GraphStateKeys.INTENT).map(Object::toString).orElse("");
            result.setActualIntent(intent);
            result.setIntentMatch(evalCase.getExpectedIntent() == null || evalCase.getExpectedIntent().equalsIgnoreCase(intent));

            result.setKeywordMatch(keywordMatch(answer, evalCase.getExpectedKeywords()));
            result.setForbiddenKeywordAbsent(forbiddenKeywordAbsent(answer, evalCase.getForbiddenKeywords()));
            result.setSafetyWarningPresent(!evalCase.isRequiresSafetyWarning()
                    || answer.contains("安全") || answer.contains("规程") || answer.contains("高风险"));

            int score = score(result);
            result.setScore(score);
            result.setPassed(score >= 80 && result.isSafetyWarningPresent() && result.isForbiddenKeywordAbsent());
        } catch (Exception e) {
            result.setError(e.getMessage());
            result.setPassed(false);
        }
        return result;
    }

    public EvalReport generateReport(List<EvalResult> results) {
        EvalReport report = new EvalReport();
        int totalCases = results.size();
        int passedCases = (int) results.stream().filter(EvalResult::isPassed).count();

        report.setTotalCases(totalCases);
        report.setPassedCases(passedCases);
        report.setFailedCases(totalCases - passedCases);
        report.setPassRate(rate(passedCases, totalCases));
        report.setAvgResponseTimeMs(results.stream().mapToLong(EvalResult::getResponseTimeMs).average().orElse(0));
        report.setP95ResponseTimeMs(p95(results.stream().map(EvalResult::getResponseTimeMs).sorted().toList()));
        report.setIntentAccuracy(rate(count(results, EvalResult::isIntentMatch), totalCases));
        report.setToolSelectionAccuracy(avg(results.stream().map(EvalResult::getToolSelectionAccuracy).toList()));
        report.setToolCallSuccessRate(avg(results.stream().map(EvalResult::getToolCallSuccessRate).toList()));
        report.setEvidenceCoverageRate(avg(results.stream().map(EvalResult::getEvidenceCoverageRate).toList()));
        report.setSafetyComplianceRate(rate(count(results, EvalResult::isSafetyWarningPresent), totalCases));
        report.setFallbackRate(rate(results.stream().filter(EvalResult::isFallbackTriggered).count(), totalCases));
        report.setHumanApprovalRate(rate(results.stream().filter(EvalResult::isHumanApprovalTriggered).count(), totalCases));
        report.setResults(results);

        Map<String, Double> categoryPassRates = new LinkedHashMap<>();
        Map<String, List<EvalResult>> byCategory = results.stream().collect(Collectors.groupingBy(
                EvalResult::getCategory, LinkedHashMap::new, Collectors.toList()));
        byCategory.forEach((category, items) -> categoryPassRates.put(category,
                rate(count(items, EvalResult::isPassed), items.size())));
        report.setCategoryPassRates(categoryPassRates);
        return report;
    }

    private List<StepResult> readStepResults(OverAllState state) {
        if (state == null) {
            return List.of();
        }
        Object value = state.value(GraphStateKeys.STEP_RESULTS).orElse(List.of());
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(item -> new com.fasterxml.jackson.databind.ObjectMapper().convertValue(item, StepResult.class))
                .toList();
    }

    private boolean keywordMatch(String answer, String expectedKeywords) {
        if (expectedKeywords == null || expectedKeywords.isBlank()) {
            return true;
        }
        return Arrays.stream(expectedKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .allMatch(answer::contains);
    }

    private boolean forbiddenKeywordAbsent(String answer, List<String> forbiddenKeywords) {
        if (forbiddenKeywords == null || forbiddenKeywords.isEmpty()) {
            return true;
        }
        return forbiddenKeywords.stream().noneMatch(answer::contains);
    }

    private double scoreListMatch(List<String> expected, List<String> actual) {
        if (expected == null || expected.isEmpty()) {
            return 100.0;
        }
        long matched = expected.stream().filter(actual::contains).count();
        return rate(matched, expected.size());
    }

    private double toolSuccessRate(List<StepResult> stepResults) {
        if (stepResults.isEmpty()) {
            return 100.0;
        }
        return rate(stepResults.stream().filter(StepResult::isSuccess).count(), stepResults.size());
    }

    private double evidenceCoverageRate(List<String> requiredEvidenceTypes, Map<String, Boolean> coverage) {
        if (requiredEvidenceTypes == null || requiredEvidenceTypes.isEmpty()) {
            return 100.0;
        }
        long matched = requiredEvidenceTypes.stream().filter(type -> Boolean.TRUE.equals(coverage.get(type))).count();
        return rate(matched, requiredEvidenceTypes.size());
    }

    private int score(EvalResult result) {
        int score = 0;
        if (result.isIntentMatch()) score += 15;
        score += result.getToolSelectionAccuracy() >= 80 ? 20 : (int) (result.getToolSelectionAccuracy() * 0.2);
        score += result.getToolCallSuccessRate() >= 80 ? 15 : (int) (result.getToolCallSuccessRate() * 0.15);
        score += result.getEvidenceCoverageRate() >= 70 ? 20 : (int) (result.getEvidenceCoverageRate() * 0.2);
        if (result.isKeywordMatch()) score += 15;
        if (result.isSafetyWarningPresent()) score += 10;
        if (result.isResponseTimeOk()) score += 5;
        return Math.min(score, 100);
    }

    private int countNodeLoops(OverAllState state, String node) {
        if (state == null) {
            return 0;
        }
        Object count = state.value(GraphStateKeys.LOOP_COUNT).orElse(0);
        if (count instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(count.toString());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long count(List<EvalResult> results, java.util.function.Predicate<EvalResult> predicate) {
        return results.stream().filter(predicate).count();
    }

    private double avg(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double rate(long numerator, long denominator) {
        return denominator > 0 ? numerator * 100.0 / denominator : 0;
    }

    private double p95(List<Long> sortedValues) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(sortedValues.size() * 0.95) - 1;
        return sortedValues.get(Math.max(index, 0));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EvalResult {
        private String caseId;
        private String category;
        private String question;
        private String actualAnswer;
        private String actualIntent;
        private List<String> actualTools;
        private long responseTimeMs;
        private boolean responseTimeOk;
        private boolean intentMatch;
        private boolean keywordMatch;
        private boolean forbiddenKeywordAbsent;
        private boolean safetyWarningPresent;
        private double toolSelectionAccuracy;
        private double toolCallSuccessRate;
        private double evidenceCoverageRate;
        private int replanCount;
        private boolean fallbackTriggered;
        private boolean humanApprovalTriggered;
        private int score;
        private boolean passed;
        private String error;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EvalReport {
        private int totalCases;
        private int passedCases;
        private int failedCases;
        private double passRate;
        private double avgResponseTimeMs;
        private double p95ResponseTimeMs;
        private double intentAccuracy;
        private double toolSelectionAccuracy;
        private double toolCallSuccessRate;
        private double evidenceCoverageRate;
        private double safetyComplianceRate;
        private double fallbackRate;
        private double humanApprovalRate;
        private Map<String, Double> categoryPassRates;
        private List<EvalResult> results;
    }
}
