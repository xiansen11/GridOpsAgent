package org.example.controller;

import org.example.observability.ObservabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    @Autowired
    private ObservabilityService observabilityService;

    @GetMapping("/traces/{traceId}")
    public Map<String, Object> getTrace(@PathVariable String traceId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", traceId);
        response.put("summary", observabilityService.getTraceSummary(traceId));
        response.put("agentSteps", observabilityService.getTrace(traceId));
        response.put("toolCalls", observabilityService.getToolCallLogs(traceId));
        return response;
    }
}
