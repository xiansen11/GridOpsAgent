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
public class PowerAlarmHistoryTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerAlarmHistoryTools.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Tool(description = "查询电力设备历史告警记录。可以按设备、告警类型、时间范围等条件查询。" +
            "适用于了解设备过去的告警情况、判断告警是否重复出现等场景。")
    public String getAlarmHistory(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "告警类型过滤，如 油温异常,局放异常。为空返回所有类型") String alarmType,
            @ToolParam(description = "查询时间范围，如 24h, 7d, 30d。默认7d") String timeRange,
            @ToolParam(description = "返回记录数量，默认10") Integer limit) {

        logger.info("查询历史告警: deviceId={}, alarmType={}, timeRange={}", deviceId, alarmType, timeRange);
        int actualLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                List<Map<String, Object>> alarms = new ArrayList<>();
                Instant now = Instant.now();

                Map<String, Object> alarm1 = new LinkedHashMap<>();
                alarm1.put("alarmId", "ALM202510180032");
                alarm1.put("alarmTime", FORMATTER.format(now.minus(2, ChronoUnit.HOURS)));
                alarm1.put("alarmType", "油温异常升高");
                alarm1.put("alarmLevel", "重要");
                alarm1.put("currentValue", "86℃");
                alarm1.put("threshold", "80℃");
                alarm1.put("status", "未处理");
                alarms.add(alarm1);

                Map<String, Object> alarm2 = new LinkedHashMap<>();
                alarm2.put("alarmId", "ALM202510050018");
                alarm2.put("alarmTime", FORMATTER.format(now.minus(15, ChronoUnit.DAYS)));
                alarm2.put("alarmType", "冷却器控制回路异常");
                alarm2.put("alarmLevel", "一般");
                alarm2.put("status", "已处理");
                alarms.add(alarm2);

                Map<String, Object> alarm3 = new LinkedHashMap<>();
                alarm3.put("alarmId", "ALM202509030025");
                alarm3.put("alarmTime", FORMATTER.format(now.minus(45, ChronoUnit.DAYS)));
                alarm3.put("alarmType", "温控器动作不灵敏");
                alarm3.put("alarmLevel", "一般");
                alarm3.put("status", "已处理");
                alarms.add(alarm3);

                result.put("alarms", alarms);
                result.put("total", alarms.size());
            } else {
                result.put("message", "真实模式需要接入监控告警系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询历史告警失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
