package org.example.graph.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class ToolResultValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidationResult validate(String toolName, String result) {
        if (result == null || result.isBlank()) {
            return ValidationResult.invalid("工具返回为空", "EMPTY_TOOL_RESULT", true);
        }

        String lower = result.toLowerCase(Locale.ROOT);
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return ValidationResult.invalid("工具调用超时", "TOOL_TIMEOUT", true);
        }
        if (lower.contains("unauthorized") || lower.contains("forbidden")) {
            return ValidationResult.invalid("工具调用未授权", "TOOL_UNAUTHORIZED", false);
        }
        if (lower.contains("\"error\"") || lower.contains("failed") || lower.contains("失败")) {
            return ValidationResult.invalid("工具返回错误结果", "TOOL_ERROR", true);
        }

        JsonNode json = null;
        if (looksLikeJson(result)) {
            try {
                json = objectMapper.readTree(result);
            } catch (Exception e) {
                return ValidationResult.invalid("工具返回不是合法 JSON: " + e.getMessage(), "INVALID_TOOL_JSON", true);
            }
        }

        ValidationResult shapeResult = validateShape(toolName, json, result);
        if (!shapeResult.isValid()) {
            return shapeResult;
        }
        return ValidationResult.ok();
    }

    public String evidenceType(String toolName) {
        return switch (toolName == null ? "" : toolName) {
            case "getDeviceProfile" -> "DEVICE_PROFILE";
            case "getDeviceStatus" -> "DEVICE_STATUS";
            case "getAlarmHistory" -> "ALARM_HISTORY";
            case "getDeviceLogs" -> "DEVICE_LOGS";
            case "getDefectTickets" -> "DEFECT_TICKETS";
            case "searchSafetyRules" -> "SAFETY_RULES";
            case "queryInternalDocs" -> "RAG_DOCS";
            default -> "OTHER";
        };
    }

    private ValidationResult validateShape(String toolName, JsonNode json, String raw) {
        if (json == null || toolName == null) {
            return ValidationResult.ok();
        }

        Map<String, String[]> requiredAny = Map.of(
                "getDeviceStatus", new String[]{"deviceId", "metrics", "status"},
                "getAlarmHistory", new String[]{"deviceId", "alarms", "total"},
                "getDeviceLogs", new String[]{"deviceId", "logs", "total"},
                "getDefectTickets", new String[]{"deviceId", "tickets", "total"},
                "getDeviceProfile", new String[]{"deviceId", "deviceName", "deviceType", "model", "manufacturer"},
                "searchSafetyRules", new String[]{"rules", "total", "query"},
                "queryInternalDocs", new String[]{"status", "content", "score"}
        );

        String[] fields = requiredAny.get(toolName);
        if (fields == null) {
            return ValidationResult.ok();
        }

        for (String field : fields) {
            if (json.has(field) || raw.contains("\"" + field + "\"")) {
                return ValidationResult.ok();
            }
        }
        return ValidationResult.invalid("工具结果缺少期望业务字段: " + toolName, "TOOL_RESULT_SHAPE_MISMATCH", true);
    }

    private boolean looksLikeJson(String result) {
        String trimmed = result.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}
