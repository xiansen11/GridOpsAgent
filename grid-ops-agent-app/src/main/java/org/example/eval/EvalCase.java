package org.example.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalCase {
    private String caseId;
    private String category;
    private String question;
    private String expectedIntent;
    private List<String> expectedTools;
    private List<String> requiredEvidenceTypes;
    private List<String> forbiddenKeywords;
    private String expectedKeywords;
    private int maxResponseTimeMs;
    private boolean requiresSafetyWarning;
    private String expectedRiskLevel;
}
