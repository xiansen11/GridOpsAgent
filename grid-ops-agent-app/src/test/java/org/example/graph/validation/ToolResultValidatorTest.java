package org.example.graph.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolResultValidatorTest {

    private final ToolResultValidator validator = new ToolResultValidator();

    @Test
    void acceptsValidDeviceStatusJson() {
        ValidationResult result = validator.validate("getDeviceStatus",
                "{\"deviceId\":\"TR-110KV-001\",\"metrics\":{\"oilTemperature\":\"86C\"}}");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void rejectsEmptyResultAsRecoverable() {
        ValidationResult result = validator.validate("getDeviceStatus", "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorType()).isEqualTo("EMPTY_TOOL_RESULT");
        assertThat(result.isRecoverable()).isTrue();
    }

    @Test
    void rejectsErrorPayload() {
        ValidationResult result = validator.validate("getDeviceLogs", "{\"error\":\"timeout\"}");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorType()).isEqualTo("TOOL_TIMEOUT");
    }
}
