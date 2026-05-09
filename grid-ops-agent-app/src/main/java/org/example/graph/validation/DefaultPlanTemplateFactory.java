package org.example.graph.validation;

import org.example.graph.model.PlanStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DefaultPlanTemplateFactory {

    public List<PlanStep> createDefaultPlan(String input, Map<String, String> entities) {
        String normalized = input == null ? "" : input.toLowerCase(Locale.ROOT);
        if (normalized.contains("油温") || normalized.contains("oil") || normalized.contains("temperature")) {
            return oilTemperaturePlan(entities);
        }
        return genericDevicePlan(entities);
    }

    public List<PlanStep> genericDevicePlan(Map<String, String> entities) {
        List<PlanStep> steps = new ArrayList<>();
        String deviceId = deviceId(entities);
        steps.add(step(1, "查询设备台账", "getDeviceProfile", Map.of("deviceId", deviceId),
                "确认设备基础信息、型号和运维属性", "返回设备台账 JSON"));
        steps.add(step(2, "查询当前状态", "getDeviceStatus", Map.of("deviceId", deviceId, "metrics", "", "timeRange", "1h"),
                "确认设备当前运行状态", "返回实时状态和关键指标"));
        steps.add(step(3, "查询历史告警", "getAlarmHistory", Map.of("deviceId", deviceId, "alarmType", "", "timeRange", "24h", "limit", 10),
                "判断是否存在重复或关联告警", "返回历史告警列表"));
        steps.add(step(4, "查询运行日志", "getDeviceLogs", Map.of("deviceId", deviceId, "timeRange", "24h", "keywords", ""),
                "检查设备动作和异常日志", "返回运行日志列表"));
        steps.add(step(5, "查询缺陷工单", "getDefectTickets", Map.of("deviceId", deviceId, "defectType", "", "timeRange", "180d"),
                "确认是否存在历史缺陷", "返回缺陷工单列表"));
        steps.add(step(6, "查询内部处理经验", "queryInternalDocs", Map.of("query", deviceId + " 设备异常 处理经验"),
                "补充内部知识库经验", "返回相关知识文档"));
        steps.add(step(7, "查询安全规程", "searchSafetyRules", Map.of("query", deviceId + " 设备异常 安全处置", "ruleType", "安规"),
                "确认处置安全要求", "返回安全规程条款"));
        return steps;
    }

    public List<PlanStep> oilTemperaturePlan(Map<String, String> entities) {
        List<PlanStep> steps = new ArrayList<>();
        String deviceId = deviceId(entities);
        steps.add(step(1, "查询设备型号、容量、投运时间", "getDeviceProfile", Map.of("deviceId", deviceId),
                "确认变压器基础台账和冷却方式", "返回设备台账 JSON"));
        steps.add(step(2, "查询油温、负荷、冷却器状态、环境温度", "getDeviceStatus",
                Map.of("deviceId", deviceId, "metrics", "oilTemperature,loadRate,coolerStatus,environmentTemp", "timeRange", "1h"),
                "确认油温异常是否与负荷或冷却系统相关", "返回实时状态 JSON"));
        steps.add(step(3, "查询近24小时油温和冷却相关告警", "getAlarmHistory",
                Map.of("deviceId", deviceId, "alarmType", "油温,冷却器", "timeRange", "24h", "limit", 10),
                "确认告警发生顺序和重复性", "返回历史告警列表"));
        steps.add(step(4, "查询冷却器启停、风机和油泵动作日志", "getDeviceLogs",
                Map.of("deviceId", deviceId, "timeRange", "24h", "keywords", "冷却器,风机,油泵,温控器"),
                "检查冷却系统动作是否异常", "返回运行日志列表"));
        steps.add(step(5, "查询冷却系统和测温装置相关缺陷", "getDefectTickets",
                Map.of("deviceId", deviceId, "defectType", "冷却器,温控器,油温", "timeRange", "180d"),
                "确认是否存在重复缺陷或未闭环工单", "返回缺陷工单列表"));
        steps.add(step(6, "查询带电设备巡视和高温异常处置要求", "searchSafetyRules",
                Map.of("query", "变压器 油温 异常 高温 安全处置", "ruleType", "安规"),
                "补充现场处置安全边界", "返回安全规程条款"));
        return steps;
    }

    private PlanStep step(int no, String action, String toolName, Map<String, Object> params,
                          String purpose, String expected) {
        return PlanStep.builder()
                .stepId(String.format("step-%03d", no))
                .stepNo(no)
                .step(no)
                .stepType("TOOL_CALL")
                .action(action)
                .toolName(toolName)
                .tool(toolName)
                .params(params)
                .purpose(purpose)
                .expected(expected)
                .dependsOn(List.of())
                .status("PENDING")
                .retryCount(0)
                .required(true)
                .build();
    }

    private String deviceId(Map<String, String> entities) {
        if (entities != null) {
            for (String key : List.of("deviceId", "device_id", "设备编号", "deviceName", "设备名称")) {
                String value = entities.get(key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return "TR-110KV-001";
    }
}
