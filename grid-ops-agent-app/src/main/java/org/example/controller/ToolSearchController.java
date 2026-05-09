package org.example.controller;

import org.example.entity.ToolInfo;
import org.example.tool.ToolRegistryService;
import org.example.tool.ToolSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tools")
public class ToolSearchController {

    @Autowired
    private ToolRegistryService toolRegistryService;

    @Autowired
    private ToolSearchService toolSearchService;

    @GetMapping("/list")
    public List<ToolInfo> listAllTools() {
        return toolRegistryService.getAllTools();
    }

    @GetMapping("/search")
    public List<ToolInfo> searchTools(@RequestParam String keyword) {
        return toolSearchService.searchByKeyword(keyword);
    }

    @GetMapping("/search/intent")
    public List<ToolInfo> searchByIntent(@RequestParam String intent,
                                          @RequestParam(required = false) String userId) {
        if (userId != null) {
            return toolSearchService.searchByIntentWithPermission(intent, userId);
        }
        return toolSearchService.searchByIntent(intent);
    }

    @GetMapping("/search/tags")
    public List<ToolInfo> searchByTags(@RequestParam List<String> tags) {
        return toolSearchService.searchByTags(tags);
    }

    @GetMapping("/high-risk")
    public List<ToolInfo> getHighRiskTools() {
        return toolRegistryService.getHighRiskTools();
    }

    @PostMapping("/refresh")
    public Map<String, Object> refreshCache() {
        toolRegistryService.refreshCache();
        return Map.of("message", "缓存刷新完成", "toolCount", toolRegistryService.getAllTools().size());
    }
}
