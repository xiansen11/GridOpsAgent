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

        long passed = results.stream().filter(EvalService.EvalResult::isPassed).count();
        long total = results.size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", total);
        response.put("passed", passed);
        response.put("failed", total - passed);
        response.put("passRate", total > 0 ? String.format("%.1f%%", passed * 100.0 / total) : "0%");
        response.put("results", results);
        return response;
    }
}
