package org.example.powertools.config;

import org.example.powertools.tool.PowerAlarmHistoryTools;
import org.example.powertools.tool.PowerDefectTicketTools;
import org.example.powertools.tool.PowerDeviceLogsTools;
import org.example.powertools.tool.PowerDeviceProfileTools;
import org.example.powertools.tool.PowerDeviceStatusTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PowerMcpToolConfig {

    @Bean
    public ToolCallbackProvider powerMcpTools(
            PowerDeviceStatusTools powerDeviceStatusTools,
            PowerAlarmHistoryTools powerAlarmHistoryTools,
            PowerDeviceLogsTools powerDeviceLogsTools,
            PowerDefectTicketTools powerDefectTicketTools,
            PowerDeviceProfileTools powerDeviceProfileTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        powerDeviceStatusTools,
                        powerAlarmHistoryTools,
                        powerDeviceLogsTools,
                        powerDefectTicketTools,
                        powerDeviceProfileTools
                )
                .build();
    }
}
