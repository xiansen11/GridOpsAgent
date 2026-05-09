package org.example.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.entity.ToolInfo;
import org.example.mapper.ToolInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ToolRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistryService.class);

    @Autowired
    private ToolInfoMapper toolInfoMapper;

    private final Map<String, ToolInfo> toolCache = new ConcurrentHashMap<>();

    public void registerTool(String toolId, String toolName, String description,
                              String inputSchema, String outputSchema,
                              List<String> tags, String permissionLevel,
                              String riskLevel, String ownerSystem) {
        ToolInfo tool = ToolInfo.builder()
                .toolId(toolId)
                .toolName(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .tags(tags != null ? String.join(",", tags) : null)
                .permissionLevel(permissionLevel)
                .riskLevel(riskLevel)
                .ownerSystem(ownerSystem)
                .enabled(true)
                .build();

        ToolInfo existing = toolInfoMapper.selectOne(
                new LambdaQueryWrapper<ToolInfo>().eq(ToolInfo::getToolId, toolId));
        if (existing != null) {
            tool.setId(existing.getId());
            toolInfoMapper.updateById(tool);
        } else {
            toolInfoMapper.insert(tool);
        }

        toolCache.put(toolId, tool);
        logger.info("注册工具: toolId={}, name={}, risk={}", toolId, toolName, riskLevel);
    }

    public ToolInfo getTool(String toolId) {
        return toolCache.computeIfAbsent(toolId, id ->
                toolInfoMapper.selectOne(
                        new LambdaQueryWrapper<ToolInfo>().eq(ToolInfo::getToolId, id)));
    }

    public List<ToolInfo> getAllTools() {
        return toolInfoMapper.selectList(
                new LambdaQueryWrapper<ToolInfo>().eq(ToolInfo::getEnabled, true));
    }

    public List<ToolInfo> searchByTags(List<String> tags) {
        return getAllTools().stream()
                .filter(tool -> {
                    if (tool.getTags() == null) return false;
                    Set<String> toolTags = Arrays.stream(tool.getTags().split(","))
                            .map(String::trim).collect(Collectors.toSet());
                    return tags.stream().anyMatch(toolTags::contains);
                })
                .collect(Collectors.toList());
    }

    public List<ToolInfo> searchByKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return getAllTools().stream()
                .filter(tool ->
                        (tool.getToolName() != null && tool.getToolName().toLowerCase().contains(lowerKeyword)) ||
                        (tool.getDescription() != null && tool.getDescription().toLowerCase().contains(lowerKeyword)) ||
                        (tool.getTags() != null && tool.getTags().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    public List<ToolInfo> searchByPermission(String permissionLevel) {
        return getAllTools().stream()
                .filter(tool -> permissionLevel.equals(tool.getPermissionLevel()))
                .collect(Collectors.toList());
    }

    public List<ToolInfo> getHighRiskTools() {
        return getAllTools().stream()
                .filter(tool -> "HIGH".equals(tool.getRiskLevel()))
                .collect(Collectors.toList());
    }

    public void refreshCache() {
        toolCache.clear();
        List<ToolInfo> all = toolInfoMapper.selectList(null);
        all.forEach(tool -> toolCache.put(tool.getToolId(), tool));
        logger.info("工具缓存刷新完成，共 {} 个工具", toolCache.size());
    }
}
