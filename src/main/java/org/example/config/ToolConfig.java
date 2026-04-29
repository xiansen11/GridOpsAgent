package org.example.config;

import org.example.agent.tool.DateTimeTools;
import org.example.agent.tool.InternalDocsTools;
import org.example.agent.tool.power.*;
import org.example.tool.ToolRegistryService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Configuration
public class ToolConfig {

    @Autowired
    private DateTimeTools dateTimeTools;

    @Autowired
    private InternalDocsTools internalDocsTools;

    @Autowired
    private PowerDeviceStatusTools powerDeviceStatusTools;

    @Autowired
    private PowerAlarmHistoryTools powerAlarmHistoryTools;

    @Autowired
    private PowerDeviceLogsTools powerDeviceLogsTools;

    @Autowired
    private PowerDefectTicketTools powerDefectTicketTools;

    @Autowired
    private PowerSafetyRulesTools powerSafetyRulesTools;

    @Autowired
    private PowerDeviceProfileTools powerDeviceProfileTools;

    @Autowired
    private ToolRegistryService toolRegistryService;

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        dateTimeTools,
                        internalDocsTools,
                        powerDeviceStatusTools,
                        powerAlarmHistoryTools,
                        powerDeviceLogsTools,
                        powerDefectTicketTools,
                        powerSafetyRulesTools,
                        powerDeviceProfileTools
                )
                .build();
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
                "查询设备实时运行状态", null, null,
                List.of("device", "status", "monitoring"), "QUERY", "LOW", "SCADA");

        toolRegistryService.registerTool("getAlarmHistory", "getAlarmHistory",
                "查询历史告警记录", null, null,
                List.of("alarm", "history", "alert"), "QUERY", "LOW", "SCADA");

        toolRegistryService.registerTool("getDeviceLogs", "getDeviceLogs",
                "查询设备运行日志", null, null,
                List.of("log", "device", "analysis"), "QUERY", "LOW", "DMS");

        toolRegistryService.registerTool("getDefectTickets", "getDefectTickets",
                "查询缺陷工单", null, null,
                List.of("ticket", "defect", "work-order"), "QUERY", "LOW", "PMS");

        toolRegistryService.registerTool("searchSafetyRules", "searchSafetyRules",
                "检索安规条款", null, null,
                List.of("safety", "regulation", "compliance"), "QUERY", "LOW", "REGULATION");

        toolRegistryService.registerTool("getDeviceProfile", "getDeviceProfile",
                "查询设备台账信息", null, null,
                List.of("device", "profile", "asset"), "QUERY", "LOW", "PMS");
    }
}
