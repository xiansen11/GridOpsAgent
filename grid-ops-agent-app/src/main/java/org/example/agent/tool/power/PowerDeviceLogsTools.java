package org.example.agent.tool.power;

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
import java.util.*;

@Component
public class PowerDeviceLogsTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerDeviceLogsTools.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Tool(description = "查询电力设备运行日志，包括冷却器启停记录、温控器动作记录、保护动作记录等。" +
            "适用于分析设备故障前后的运行状态变化和异常事件。")
    public String getDeviceLogs(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "查询时间范围，如 1h, 24h。默认1h") String timeRange,
            @ToolParam(description = "关键词过滤，如 冷却器,启动失败,温控器。多个关键词用逗号分隔") String keywords) {

        logger.info("查询设备日志: deviceId={}, timeRange={}, keywords={}", deviceId, timeRange, keywords);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                List<Map<String, Object>> logs = new ArrayList<>();
                Instant now = Instant.now();

                Map<String, Object> log1 = new LinkedHashMap<>();
                log1.put("timestamp", FORMATTER.format(now.minus(12, ChronoUnit.MINUTES)));
                log1.put("level", "ERROR");
                log1.put("source", "冷却器#2");
                log1.put("message", "冷却器#2 风机启动失败，接触器KM2未吸合");
                logs.add(log1);

                Map<String, Object> log2 = new LinkedHashMap<>();
                log2.put("timestamp", FORMATTER.format(now.minus(7, ChronoUnit.MINUTES)));
                log2.put("level", "WARN");
                log2.put("source", "温控器");
                log2.put("message", "温控器切换失败，当前档位第2档，目标档位第3档");
                logs.add(log2);

                Map<String, Object> log3 = new LinkedHashMap<>();
                log3.put("timestamp", FORMATTER.format(now.minus(2, ChronoUnit.MINUTES)));
                log3.put("level", "ALARM");
                log3.put("source", "油温监测");
                log3.put("message", "主变油温超过告警阈值，当前86℃，阈值80℃");
                logs.add(log3);

                Map<String, Object> log4 = new LinkedHashMap<>();
                log4.put("timestamp", FORMATTER.format(now.minus(30, ChronoUnit.MINUTES)));
                log4.put("level", "INFO");
                log4.put("source", "冷却器#1");
                log4.put("message", "冷却器#1 风机正常运行，转速1450rpm");
                logs.add(log4);

                result.put("logs", logs);
                result.put("total", logs.size());
                result.put("analysis", "日志显示冷却器#2风机在油温告警前出现启动失败，随后温控器切换失败，最终油温超过阈值。初步推测本次油温异常与冷却系统故障存在较强关联。");
            } else {
                result.put("message", "真实模式需要接入设备日志系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询设备日志失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
