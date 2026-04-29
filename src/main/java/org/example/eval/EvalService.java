package org.example.eval;

import org.example.service.AgentOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EvalService {

    private static final Logger logger = LoggerFactory.getLogger(EvalService.class);

    @Autowired
    private EvalDataset evalDataset;

    @Autowired
    private AgentOrchestrationService orchestrationService;

    public List<EvalResult> runAllEvals() {
        List<EvalCase> cases = evalDataset.getAllCases();
        List<EvalResult> results = new ArrayList<>();

        for (EvalCase evalCase : cases) {
            results.add(runSingleEval(evalCase));
        }

        return results;
    }

    public EvalResult runSingleEval(EvalCase evalCase) {
        long startTime = System.currentTimeMillis();
        EvalResult result = EvalResult.builder()
                .caseId(evalCase.getCaseId())
                .category(evalCase.getCategory())
                .question(evalCase.getQuestion())
                .build();

        try {
            String answer = orchestrationService.handleChat(evalCase.getQuestion(), List.of());
            long duration = System.currentTimeMillis() - startTime;

            result.setActualAnswer(answer);
            result.setResponseTimeMs(duration);
            result.setResponseTimeOk(duration <= evalCase.getMaxResponseTimeMs());

            boolean keywordMatch = true;
            if (evalCase.getExpectedKeywords() != null) {
                for (String keyword : evalCase.getExpectedKeywords().split(",")) {
                    if (!answer.contains(keyword.trim())) {
                        keywordMatch = false;
                        break;
                    }
                }
            }
            result.setKeywordMatch(keywordMatch);

            if (evalCase.isRequiresSafetyWarning()) {
                result.setSafetyWarningPresent(
                        answer.contains("安全") || answer.contains("规程") || answer.contains("⚠️"));
            } else {
                result.setSafetyWarningPresent(true);
            }

            result.setPassed(result.isResponseTimeOk() && result.isKeywordMatch() &&
                    result.isSafetyWarningPresent());

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
        int failedCases = totalCases - passedCases;

        report.setTotalCases(totalCases);
        report.setPassedCases(passedCases);
        report.setFailedCases(failedCases);
        report.setPassRate(totalCases > 0 ? (double) passedCases / totalCases * 100 : 0);

        double avgResponseTime = results.stream()
                .mapToLong(EvalResult::getResponseTimeMs)
                .average()
                .orElse(0);
        report.setAvgResponseTimeMs(avgResponseTime);

        long keywordMatchCount = results.stream().filter(EvalResult::isKeywordMatch).count();
        report.setKeywordMatchRate(totalCases > 0 ? (double) keywordMatchCount / totalCases * 100 : 0);

        long safetyWarningCount = results.stream().filter(EvalResult::isSafetyWarningPresent).count();
        report.setSafetyWarningRate(totalCases > 0 ? (double) safetyWarningCount / totalCases * 100 : 0);

        report.setResults(results);

        Map<String, Double> categoryPassRates = new LinkedHashMap<>();
        Map<String, List<EvalResult>> byCategory = new LinkedHashMap<>();
        for (EvalResult r : results) {
            byCategory.computeIfAbsent(r.getCategory(), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<EvalResult>> entry : byCategory.entrySet()) {
            long catPassed = entry.getValue().stream().filter(EvalResult::isPassed).count();
            categoryPassRates.put(entry.getKey(), (double) catPassed / entry.getValue().size() * 100);
        }
        report.setCategoryPassRates(categoryPassRates);

        return report;
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
        private long responseTimeMs;
        private boolean responseTimeOk;
        private boolean keywordMatch;
        private boolean safetyWarningPresent;
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
        private double keywordMatchRate;
        private double safetyWarningRate;
        private Map<String, Double> categoryPassRates;
        private List<EvalResult> results;
    }
}
