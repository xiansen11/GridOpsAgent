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
public class PowerDefectTicketTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerDefectTicketTools.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    @Tool(description = "查询电力设备历史缺陷工单，包括缺陷描述、处理状态、处理结果等。")
    public String getDefectTickets(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId,
            @ToolParam(description = "缺陷类型过滤，如 冷却器,温控器,油温。为空返回所有类型") String defectType,
            @ToolParam(description = "查询时间范围，如 30d, 90d, 180d。默认180d") String timeRange) {
        logger.info("MCP getDefectTickets: deviceId={}, defectType={}, timeRange={}", deviceId, defectType, timeRange);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);
            result.put("queryTime", FORMATTER.format(Instant.now()));

            if (mockEnabled) {
                Instant now = Instant.now();
                List<Map<String, Object>> tickets = new ArrayList<>();
                tickets.add(ticket("DEF202510020001", now.minus(16, ChronoUnit.DAYS), "冷却器控制回路异常", "一般缺陷",
                        "1号主变冷却器控制回路间歇性异常，接触器KM2偶尔无法吸合", "待复查", "与本次油温异常告警相关"));
                tickets.add(ticket("DEF202509030002", now.minus(55, ChronoUnit.DAYS), "温控器动作不灵敏", "一般缺陷",
                        "1号主变温控器切换延迟，动作不灵敏，需要校准", "已处理", "与本次油温异常告警相关"));
                tickets.add(ticket("DEF202507150003", now.minus(105, ChronoUnit.DAYS), "油温偏高", "一般缺陷",
                        "高温天气下1号主变油温偏高，达到78℃，接近告警阈值", "已处理", "与本次告警类型相同"));

                result.put("tickets", tickets);
                result.put("total", tickets.size());
                result.put("analysis", "该设备近半年存在2次冷却器相关缺陷和1次油温偏高缺陷，本次油温异常与历史缺陷具有相似性，建议按重复缺陷处理，重点检查控制回路。");
            } else {
                result.put("message", "真实模式需要接入缺陷工单系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("getDefectTickets failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private Map<String, Object> ticket(String id, Instant time, String type, String level,
                                       String description, String status, String similarity) {
        Map<String, Object> ticket = new LinkedHashMap<>();
        ticket.put("ticketId", id);
        ticket.put("createTime", FORMATTER.format(time));
        ticket.put("defectType", type);
        ticket.put("defectLevel", level);
        ticket.put("description", description);
        ticket.put("status", status);
        ticket.put("similarity", similarity);
        return ticket;
    }
}
