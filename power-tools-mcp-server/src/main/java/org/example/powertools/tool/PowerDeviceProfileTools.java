package org.example.powertools.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PowerDeviceProfileTools {

    private static final Logger logger = LoggerFactory.getLogger(PowerDeviceProfileTools.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${power.mock-enabled:true}")
    private boolean mockEnabled;

    @Tool(description = "查询电力设备台账信息，包括设备型号、厂家、投运时间、技术参数等。")
    public String getDeviceProfile(
            @ToolParam(description = "设备编号，如 TR-110KV-001") String deviceId) {
        logger.info("MCP getDeviceProfile: deviceId={}", deviceId);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deviceId", deviceId);

            if (mockEnabled) {
                if (containsAny(deviceId, "TR", "主变", "110KV")) {
                    result.put("deviceName", "1号主变");
                    result.put("deviceType", "变压器");
                    result.put("model", "SFZ11-50000/110");
                    result.put("manufacturer", "XX变压器有限公司");
                    result.put("commissioningDate", "2018-06-15");
                    result.put("voltageLevel", "110kV/10kV");
                    result.put("ratedCapacity", "50000kVA");
                    result.put("coolingType", "ONAF（油浸风冷）");
                    result.put("coolerCount", "3组");
                    result.put("station", "XX变电站");
                    result.put("location", "主变区1号位");
                    result.put("lastMaintenanceDate", "2025-08-20");
                    result.put("nextMaintenanceDate", "2026-02-20");
                    result.put("warrantyExpiry", "2023-06-15");
                    result.put("status", "在运");
                } else if (containsAny(deviceId, "KG", "开关柜")) {
                    result.put("deviceName", "10kV开关柜");
                    result.put("deviceType", "开关柜");
                    result.put("model", "KYN28A-12");
                    result.put("manufacturer", "XX开关柜有限公司");
                    result.put("commissioningDate", "2019-03-10");
                    result.put("voltageLevel", "10kV");
                    result.put("station", "XX变电站");
                    result.put("status", "在运");
                } else {
                    result.put("deviceName", "未知设备");
                    result.put("deviceType", "未知");
                    result.put("status", "未找到");
                }
            } else {
                result.put("message", "真实模式需要接入设备台账系统");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            logger.error("getDeviceProfile failed", e);
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
