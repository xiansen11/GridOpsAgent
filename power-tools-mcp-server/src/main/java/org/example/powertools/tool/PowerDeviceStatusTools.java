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
public class PowerDeviceStatusTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerDeviceStatusTools.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    @Tool(description = "查询电力设备实时运行状态，包括油温、负荷率、冷却器状态、环境温度等指标。适用于变压器、开关柜、断路器等电力设备。")
    public String getDeviceStatus(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "需要查询的指标列表，如 oilTemperature,loadRate,coolerStatus。为空时返回所有指标") String metrics,
            @ToolParam(description = "查询时间范围，如 1h, 24h, 7d。默认1h") String timeRange) {
        logger.info("MCP getDeviceStatus: deviceId={}, metrics={}, timeRange={}", deviceId, metrics, timeRange);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                result.put("status", "online");
                Map<String, Object> metricsData = new LinkedHashMap<>();

                if (containsAny(deviceId, "TR", "主变")) {
                    metricsData.put("oilTemperature", "86℃");
                    metricsData.put("oilTemperatureThreshold", "80℃");
                    metricsData.put("loadRate", "92%");
                    metricsData.put("coolerStatus", "异常");
                    metricsData.put("coolerFan1", "运行");
                    metricsData.put("coolerFan2", "启动失败");
                    metricsData.put("environmentTemp", "32℃");
                    metricsData.put("oilLevel", "正常");
                    metricsData.put("windingTemp", "78℃");
                } else if (containsAny(deviceId, "KG", "开关柜")) {
                    metricsData.put("partialDischarge", "异常");
                    metricsData.put("pdValue", "50pC");
                    metricsData.put("pdThreshold", "30pC");
                    metricsData.put("cabinetTemp", "42℃");
                    metricsData.put("humidity", "65%");
                    metricsData.put("busbarTemp", "55℃");
                } else {
                    metricsData.put("status", "运行");
                    metricsData.put("loadRate", "75%");
                    metricsData.put("temperature", "45℃");
                }

                result.put("metrics", metricsData);

                Instant now = Instant.now();
                List<Map<String, Object>> trend = new ArrayList<>();
                for (int i = 5; i >= 0; i--) {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("time", FORMATTER.format(now.minus(i * 10L, ChronoUnit.MINUTES)));
                    point.put("oilTemperature", 82 + (5 - i) * 0.8);
                    point.put("loadRate", 88 + (5 - i) * 0.8);
                    trend.add(point);
                }
                result.put("trend", trend);
            } else {
                result.put("message", "真实模式需要接入监控告警系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("getDeviceStatus failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null) {
            return false;
        }
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
