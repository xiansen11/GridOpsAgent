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
public class PowerDefectTicketTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerDefectTicketTools.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Tool(description = "查询电力设备历史缺陷工单，包括缺陷描述、处理状态、处理结果等。" +
            "适用于了解设备历史缺陷情况、判断当前告警是否与历史缺陷相关、辅助故障诊断。")
    public String getDefectTickets(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "缺陷类型过滤，如 冷却器,温控器,油温。为空返回所有类型") String defectType,
            @ToolParam(description = "查询时间范围，如 30d, 90d, 180d。默认180d") String timeRange) {

        logger.info("查询缺陷工单: deviceId={}, defectType={}, timeRange={}", deviceId, defectType, timeRange);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                List<Map<String, Object>> tickets = new ArrayList<>();
                Instant now = Instant.now();

                Map<String, Object> ticket1 = new LinkedHashMap<>();
                ticket1.put("ticketId", "DEF202510020001");
                ticket1.put("createTime", FORMATTER.format(now.minus(16, ChronoUnit.DAYS)));
                ticket1.put("defectType", "冷却器控制回路异常");
                ticket1.put("defectLevel", "一般缺陷");
                ticket1.put("description", "1号主变冷却器控制回路间歇性异常，接触器KM2偶尔无法吸合");
                ticket1.put("status", "待复查");
                ticket1.put("similarity", "与本次油温异常告警相关");
                tickets.add(ticket1);

                Map<String, Object> ticket2 = new LinkedHashMap<>();
                ticket2.put("ticketId", "DEF202509030002");
                ticket2.put("createTime", FORMATTER.format(now.minus(55, ChronoUnit.DAYS)));
                ticket2.put("defectType", "温控器动作不灵敏");
                ticket2.put("defectLevel", "一般缺陷");
                ticket2.put("description", "1号主变温控器切换延迟，动作不灵敏，需校准");
                ticket2.put("status", "已处理");
                ticket2.put("similarity", "与本次油温异常告警相关");
                tickets.add(ticket2);

                Map<String, Object> ticket3 = new LinkedHashMap<>();
                ticket3.put("ticketId", "DEF202507150003");
                ticket3.put("createTime", FORMATTER.format(now.minus(105, ChronoUnit.DAYS)));
                ticket3.put("defectType", "油温偏高");
                ticket3.put("defectLevel", "一般缺陷");
                ticket3.put("description", "高温天气下1号主变油温偏高，达到78℃，接近告警阈值");
                ticket3.put("status", "已处理");
                ticket3.put("similarity", "与本次告警类型相同");
                tickets.add(ticket3);

                result.put("tickets", tickets);
                result.put("total", tickets.size());
                result.put("analysis", "该设备近半年存在2次冷却器相关缺陷和1次油温偏高缺陷，本次油温异常与历史缺陷具有相似性，建议按重复缺陷处理，重点检查控制回路。");
            } else {
                result.put("message", "真实模式需要接入缺陷工单系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询缺陷工单失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
