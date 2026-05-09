package org.example.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.example.agent.tool.DateTimeTools;
import org.example.agent.tool.InternalDocsTools;
import org.example.agent.tool.power.PowerSafetyRulesTools;
import org.example.tool.ToolRegistryService;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ToolConfig {

    @Autowired
    private DateTimeTools dateTimeTools;

    @Autowired
    private InternalDocsTools internalDocsTools;

    @Autowired
    private PowerSafetyRulesTools powerSafetyRulesTools;

    @Autowired
    private ToolRegistryService toolRegistryService;

    @Bean
    @Primary
    public ToolCallbackProvider toolCallbackProvider(ObjectProvider<List<McpSyncClient>> mcpSyncClientsProvider) {
        ToolCallbackProvider localTools = MethodToolCallbackProvider.builder()
                .toolObjects(
                        dateTimeTools,
                        internalDocsTools,
                        powerSafetyRulesTools
                )
                .build();

        List<ToolCallback> callbacks = new ArrayList<>(List.of(localTools.getToolCallbacks()));
        List<McpSyncClient> mcpSyncClients = mcpSyncClientsProvider.getIfAvailable(List::of);
        if (!mcpSyncClients.isEmpty()) {
            ToolCallbackProvider mcpTools = new SyncMcpToolCallbackProvider(mcpSyncClients);
            callbacks.addAll(List.of(mcpTools.getToolCallbacks()));
        }

        return () -> callbacks.toArray(ToolCallback[]::new);
    }

    @PostConstruct
    public void registerToolsToRegistry() {
        toolRegistryService.registerTool("getCurrentDateTime", "getCurrentDateTime",
                "获取当前日期时间", null, null,
                List.of("time", "date"), "QUERY", "LOW", "SYSTEM");

        toolRegistryService.registerTool("queryInternalDocs", "queryInternalDocs",
                "查询内部知识文档", null, null,
                List.of("knowledge", "docs", "rag"), "QUERY", "LOW", "RAG");

        toolRegistryService.registerTool("getDeviceStatus", "getDeviceStatus",
                "通过 MCP Server 查询设备实时运行状态", null, null,
                List.of("device", "status", "monitoring", "mcp"), "QUERY", "LOW", "SCADA");

        toolRegistryService.registerTool("getAlarmHistory", "getAlarmHistory",
                "通过 MCP Server 查询历史告警记录", null, null,
                List.of("alarm", "history", "alert", "mcp"), "QUERY", "LOW", "SCADA");

        toolRegistryService.registerTool("getDeviceLogs", "getDeviceLogs",
                "通过 MCP Server 查询设备运行日志", null, null,
                List.of("log", "device", "analysis", "mcp"), "QUERY", "LOW", "DMS");

        toolRegistryService.registerTool("getDefectTickets", "getDefectTickets",
                "通过 MCP Server 查询缺陷工单", null, null,
                List.of("ticket", "defect", "work-order", "mcp"), "QUERY", "LOW", "PMS");

        toolRegistryService.registerTool("searchSafetyRules", "searchSafetyRules",
                "检索安规条款", null, null,
                List.of("safety", "regulation", "compliance"), "QUERY", "LOW", "REGULATION");

        toolRegistryService.registerTool("getDeviceProfile", "getDeviceProfile",
                "通过 MCP Server 查询设备台账信息", null, null,
                List.of("device", "profile", "asset", "mcp"), "QUERY", "LOW", "PMS");
    }
}
