package org.example.graph.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    private int score;
    private String errorType;
    private boolean recoverable;

    public static ValidationResult ok() {
        return ValidationResult.builder().valid(true).build();
    }

    public static ValidationResult warn(String warning) {
        ValidationResult result = ok();
        result.getWarnings().add(warning);
        return result;
    }

    public static ValidationResult invalid(String error, String errorType, boolean recoverable) {
        ValidationResult result = ValidationResult.builder()
                .valid(false)
                .errorType(errorType)
                .recoverable(recoverable)
                .build();
        result.getErrors().add(error);
        return result;
    }
}
