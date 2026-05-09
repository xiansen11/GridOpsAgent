package org.example.graph.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InputValidator {

    private static final int MAX_INPUT_LENGTH = 2000;
    private static final String[] DANGEROUS_KEYWORDS = {
            "DROP TABLE", "DELETE FROM", "INSERT INTO", "UPDATE ", "--", ";--",
            "UNION SELECT", "OR 1=1", "<script", "javascript:"
    };

    public CleanInput validateAndClean(String input) {
        List<String> warnings = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return new CleanInput("", ValidationResult.invalid("输入内容为空，无法继续处理", "EMPTY_INPUT", false));
        }

        String cleaned = input.trim().replaceAll("<[^>]*>", "");
        if (cleaned.length() > MAX_INPUT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_INPUT_LENGTH);
            warnings.add("输入内容超过长度限制，已截断到 " + MAX_INPUT_LENGTH + " 字符");
        }

        for (String keyword : DANGEROUS_KEYWORDS) {
            if (cleaned.toUpperCase().contains(keyword.toUpperCase())) {
                cleaned = cleaned.replace(keyword, "[FILTERED]");
                cleaned = cleaned.replace(keyword.toLowerCase(), "[FILTERED]");
                warnings.add("输入包含潜在危险片段，已过滤: " + keyword);
            }
        }

        ValidationResult result = ValidationResult.ok();
        result.setWarnings(warnings);
        return new CleanInput(cleaned, result);
    }

    public record CleanInput(String cleanedInput, ValidationResult validationResult) {
    }
}
