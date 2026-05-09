package org.example.controller;

import org.example.eval.EvalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/eval")
public class EvalController {

    @Autowired
    private EvalService evalService;

    @PostMapping("/run")
    public Map<String, Object> runEvals() {
        List<EvalService.EvalResult> results = evalService.runAllEvals();
        EvalService.EvalReport report = evalService.generateReport(results);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCases", report.getTotalCases());
        response.put("passedCases", report.getPassedCases());
        response.put("failedCases", report.getFailedCases());
        response.put("passRate", report.getPassRate());
        response.put("intentAccuracy", report.getIntentAccuracy());
        response.put("toolSelectionAccuracy", report.getToolSelectionAccuracy());
        response.put("toolCallSuccessRate", report.getToolCallSuccessRate());
        response.put("evidenceCoverageRate", report.getEvidenceCoverageRate());
        response.put("safetyComplianceRate", report.getSafetyComplianceRate());
        response.put("avgResponseTimeMs", report.getAvgResponseTimeMs());
        response.put("p95ResponseTimeMs", report.getP95ResponseTimeMs());
        response.put("fallbackRate", report.getFallbackRate());
        response.put("humanApprovalRate", report.getHumanApprovalRate());
        response.put("categoryPassRates", report.getCategoryPassRates());
        response.put("results", report.getResults());
        return response;
    }
}
