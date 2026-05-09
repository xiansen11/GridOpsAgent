package org.example.powertools.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PowerDeviceLogsTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerDeviceLogsTools.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    @Tool(description = "查询电力设备运行日志，包括冷却器启停记录、温控器动作记录、保护动作记录等。")
    public String getDeviceLogs(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "查询时间范围，如 1h, 24h。默认1h") String timeRange,
            @ToolParam(description = "关键词过滤，如 冷却器,启动失败,温控器。多个关键词用逗号分隔") String keywords) {
        logger.info("MCP getDeviceLogs: deviceId={}, timeRange={}, keywords={}", deviceId, timeRange, keywords);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                Instant now = Instant.now();
                List<Map<String, Object>> logs = new ArrayList<>();
                logs.add(log(now.minus(12, ChronoUnit.MINUTES), "ERROR", "冷却器#2", "冷却器#2 风机启动失败，接触器KM2未吸合"));
                logs.add(log(now.minus(7, ChronoUnit.MINUTES), "WARN", "温控器", "温控器切换失败，当前档位第2档，目标档位第3档"));
                logs.add(log(now.minus(2, ChronoUnit.MINUTES), "ALARM", "油温监测", "主变油温超过告警阈值，当前86℃，阈值80℃"));
                logs.add(log(now.minus(30, ChronoUnit.MINUTES), "INFO", "冷却器#1", "冷却器#1 风机正常运行，转速1450rpm"));

                result.put("logs", logs);
                result.put("total", logs.size());
                result.put("analysis", "日志显示冷却器#2风机在油温告警前出现启动失败，随后温控器切换失败，最终油温超过阈值。初步推测本次油温异常与冷却系统故障存在较强关联。");
            } else {
                result.put("message", "真实模式需要接入设备日志系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("getDeviceLogs failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private Map<String, Object> log(Instant timestamp, String level, String source, String message) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("timestamp", FORMATTER.format(timestamp));
        log.put("level", level);
        log.put("source", source);
        log.put("message", message);
        return log;
    }
}
