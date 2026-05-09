package org.example.graph.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DiagnosisValidator {

    public ValidationResult validate(String diagnosis) {
        ValidationResult result = ValidationResult.ok();
        if (diagnosis == null || diagnosis.isBlank()) {
            return ValidationResult.invalid("诊断结果为空", "EMPTY_DIAGNOSIS", true);
        }

        List<String> requiredHints = List.of("告警", "证据", "原因", "风险", "建议", "安全");
        List<String> missing = new ArrayList<>();
        for (String hint : requiredHints) {
            if (!diagnosis.contains(hint)) {
                missing.add(hint);
            }
        }
        if (!missing.isEmpty()) {
            result.setValid(false);
            result.setRecoverable(true);
            result.setErrorType("INCOMPLETE_DIAGNOSIS");
            result.getWarnings().add("诊断结果可能缺少章节: " + String.join(",", missing));
        }
        return result;
    }
}
