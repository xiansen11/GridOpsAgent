package org.example.agent.tool.power;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PowerSafetyRulesTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerSafetyRulesTools.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    @Tool(description = "查询电力安全工作规程和作业安全要求。" +
            "适用于高压室作业安全、倒闸操作安全、设备巡检安全、带电作业安全等场景。" +
            "当用户询问安全注意事项、操作规程、安全措施等问题时使用此工具。")
    public String searchSafetyRules(
            @ToolParam(description = "查询内容，如 高压室作业安全,倒闸操作,设备巡检安全措施") String query,
            @ToolParam(description = "规程类型过滤，如 安规,运维规程,抢修手册。为空返回所有类型") String ruleType) {

        logger.info("查询安规: query={}, ruleType={}", query, ruleType);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("query", query);

            if (mockEnabled) {
                List<Map<String, Object>> rules = new ArrayList<>();

                if (query.contains("高压室") || query.contains("安全措施") || query.contains("安全事项")) {
                    Map<String, Object> rule1 = new LinkedHashMap<>();
                    rule1.put("ruleId", "DL5009.3-2013-4.2.1");
                    rule1.put("source", "电力安全工作规程 变电部分");
                    rule1.put("chapter", "第四章 高压设备工作的基本要求");
                    rule1.put("content", "进入高压室前，应确认工作票和许可手续齐全，核对设备名称和编号，确认现场安全措施已布置到位。进入前应检查安全工器具状态，按要求佩戴安全帽、绝缘鞋等防护用品。");
                    rule1.put("safetyLevel", "强制");
                    rules.add(rule1);

                    Map<String, Object> rule2 = new LinkedHashMap<>();
                    rule2.put("ruleId", "DL5009.3-2013-4.2.3");
                    rule2.put("source", "电力安全工作规程 变电部分");
                    rule2.put("chapter", "第四章 高压设备工作的基本要求");
                    rule2.put("content", "严禁擅自移动遮栏、标示牌或越过安全围栏。涉及带电设备附近作业时，应保持规定安全距离，并服从现场负责人统一指挥。");
                    rule2.put("safetyLevel", "强制");
                    rules.add(rule2);
                }

                if (query.contains("油温") || query.contains("变压器") || query.contains("冷却")) {
                    Map<String, Object> rule3 = new LinkedHashMap<>();
                    rule3.put("ruleId", "YXGS-BY-03");
                    rule3.put("source", "变压器运行规程");
                    rule3.put("chapter", "第三章 变压器运行监视");
                    rule3.put("content", "当主变油温异常升高时，应检查负荷情况、冷却器运行状态、油位和温控装置。如油温继续升高超过限值，应按规程采取降负荷措施。严禁在油温超过允许值时继续运行。");
                    rule3.put("safetyLevel", "强制");
                    rules.add(rule3);
                }

                if (query.contains("巡检") || query.contains("检查")) {
                    Map<String, Object> rule4 = new LinkedHashMap<>();
                    rule4.put("ruleId", "XJGS-001");
                    rule4.put("source", "变电运维规程");
                    rule4.put("chapter", "巡检作业指导书");
                    rule4.put("content", "巡检时应按照标准化作业指导书执行，重点检查设备外观、油位油温、声音振动、渗漏油、接地引下线、标识标牌等。发现异常应立即报告并记录。");
                    rule4.put("safetyLevel", "建议");
                    rules.add(rule4);
                }

                if (rules.isEmpty()) {
                    Map<String, Object> defaultRule = new LinkedHashMap<>();
                    defaultRule.put("ruleId", "DL5009.3-2013-1.1");
                    defaultRule.put("source", "电力安全工作规程");
                    defaultRule.put("content", "电力作业必须遵守安全工作规程，作业前确认安全措施到位，作业中严格执行操作规程，作业后确认设备恢复正常状态。");
                    defaultRule.put("safetyLevel", "强制");
                    rules.add(defaultRule);
                }

                result.put("rules", rules);
                result.put("total", rules.size());
            } else {
                result.put("message", "真实模式需要接入安规查询系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询安规失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
