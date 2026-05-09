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
public class PowerAlarmHistoryTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerAlarmHistoryTools.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    @Tool(description = "查询电力设备历史告警记录。可以按设备、告警类型、时间范围等条件查询。")
    public String getAlarmHistory(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "告警类型过滤，如 油温异常,局放异常。为空返回所有类型") String alarmType,
            @ToolParam(description = "查询时间范围，如 24h, 7d, 30d。默认7d") String timeRange,
            @ToolParam(description = "返回记录数量，默认10") Integer limit) {
        logger.info("MCP getAlarmHistory: deviceId={}, alarmType={}, timeRange={}", deviceId, alarmType, timeRange);
        int actualLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                List<Map<String, Object>> alarms = new ArrayList<>();
                Instant now = Instant.now();

                alarms.add(alarm("ALM202510180032", now.minus(2, ChronoUnit.HOURS), "油温异常升高", "重要", "86℃", "80℃", "未处理"));
                alarms.add(alarm("ALM202510050018", now.minus(15, ChronoUnit.DAYS), "冷却器控制回路异常", "一般", null, null, "已处理"));
                alarms.add(alarm("ALM202509030025", now.minus(45, ChronoUnit.DAYS), "温控器动作不灵敏", "一般", null, null, "已处理"));

                result.put("alarms", alarms.stream().limit(actualLimit).toList());
                result.put("total", Math.min(alarms.size(), actualLimit));
            } else {
                result.put("message", "真实模式需要接入监控告警系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("getAlarmHistory failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private Map<String, Object> alarm(String id, Instant time, String type, String level,
                                      String currentValue, String threshold, String status) {
        Map<String, Object> alarm = new LinkedHashMap<>();
        alarm.put("alarmId", id);
        alarm.put("alarmTime", FORMATTER.format(time));
        alarm.put("alarmType", type);
        alarm.put("alarmLevel", level);
        if (currentValue != null) {
            alarm.put("currentValue", currentValue);
        }
        if (threshold != null) {
            alarm.put("threshold", threshold);
        }
        alarm.put("status", status);
        return alarm;
    }
}
