package org.example.graph.validation;

import org.example.graph.model.StepResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EvidenceQualityEvaluator {

    private static final Map<String, Integer> WEIGHTS = Map.of(
            "DEVICE_PROFILE", 15,
            "DEVICE_STATUS", 25,
            "ALARM_HISTORY", 20,
            "DEVICE_LOGS", 20,
            "DEFECT_TICKETS", 10,
            "SAFETY_RULES", 10,
            "RAG_DOCS", 10
    );

    public EvidenceScore evaluate(List<StepResult> stepResults, String evidence) {
        Map<String, Boolean> coverage = new LinkedHashMap<>();
        WEIGHTS.keySet().forEach(type -> coverage.put(type, false));

        if (stepResults != null) {
            for (StepResult result : stepResults) {
                if (Boolean.TRUE.equals(result.getMatchExpected()) || result.isSuccess()) {
                    String evidenceType = result.getEvidenceType();
                    if (evidenceType != null && coverage.containsKey(evidenceType)) {
                        coverage.put(evidenceType, true);
                    }
                }
            }
        }

        String evidenceText = evidence == null ? "" : evidence;
        markByText(coverage, evidenceText);

        int score = coverage.entrySet().stream()
                .filter(Map.Entry::getValue)
                .mapToInt(entry -> WEIGHTS.getOrDefault(entry.getKey(), 0))
                .sum();

        List<String> warnings = new ArrayList<>();
        if (!coverage.get("DEVICE_STATUS")) {
            warnings.add("缺少实时状态证据");
        }
        if (!coverage.get("ALARM_HISTORY")) {
            warnings.add("缺少历史告警证据");
        }
        if (!coverage.get("DEVICE_LOGS")) {
            warnings.add("缺少运行日志证据");
        }
        if (!coverage.get("SAFETY_RULES")) {
            warnings.add("缺少安全规程证据");
        }

        String decision = score >= 70 ? "SUFFICIENT" : score >= 40 ? "NEED_MORE" : "INSUFFICIENT";
        return new EvidenceScore(score, coverage, warnings, decision);
    }

    private void markByText(Map<String, Boolean> coverage, String evidence) {
        if (evidence.contains("getDeviceProfile") || evidence.contains("设备台账") || evidence.contains("deviceName")) {
            coverage.put("DEVICE_PROFILE", true);
        }
        if (evidence.contains("getDeviceStatus") || evidence.contains("实时状态") || evidence.contains("metrics")) {
            coverage.put("DEVICE_STATUS", true);
        }
        if (evidence.contains("getAlarmHistory") || evidence.contains("历史告警") || evidence.contains("alarms")) {
            coverage.put("ALARM_HISTORY", true);
        }
        if (evidence.contains("getDeviceLogs") || evidence.contains("运行日志") || evidence.contains("logs")) {
            coverage.put("DEVICE_LOGS", true);
        }
        if (evidence.contains("getDefectTickets") || evidence.contains("缺陷工单") || evidence.contains("tickets")) {
            coverage.put("DEFECT_TICKETS", true);
        }
        if (evidence.contains("searchSafetyRules") || evidence.contains("安全规程") || evidence.contains("rules")) {
            coverage.put("SAFETY_RULES", true);
        }
        if (evidence.contains("queryInternalDocs") || evidence.contains("知识库") || evidence.contains("RAG")) {
            coverage.put("RAG_DOCS", true);
        }
    }

    public record EvidenceScore(int score, Map<String, Boolean> coverage, List<String> warnings, String decision) {
    }
}
