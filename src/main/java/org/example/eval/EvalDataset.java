package org.example.eval;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvalDataset {

    private final List<EvalCase> cases = new ArrayList<>();

    @PostConstruct
    public void init() {
        cases.add(EvalCase.builder()
                .caseId("EVAL-001").category("安规问答")
                .question("进入高压室前需要注意什么？")
                .expectedIntent("SAFETY_QA")
                .expectedTools(List.of("searchSafetyRules", "queryInternalDocs"))
                .expectedKeywords("安全帽,绝缘鞋,工作票,安全措施")
                .maxResponseTimeMs(10000).requiresSafetyWarning(true)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-002").category("设备状态查询")
                .question("1号主变现在油温是多少？")
                .expectedIntent("DEVICE_STATUS")
                .expectedTools(List.of("getDeviceStatus"))
                .expectedKeywords("油温,℃")
                .maxResponseTimeMs(8000).requiresSafetyWarning(false)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-003").category("故障诊断")
                .question("1号主变油温异常，帮我分析原因")
                .expectedIntent("FAULT_DIAGNOSIS")
                .expectedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs", "getDefectTickets"))
                .expectedKeywords("冷却器,负荷,诊断,建议")
                .maxResponseTimeMs(60000).requiresSafetyWarning(true)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-004").category("告警诊断")
                .question("TR-110KV-001油温86℃超过阈值80℃")
                .expectedIntent("ALARM_DIAGNOSIS")
                .expectedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs"))
                .expectedKeywords("告警摘要,可能原因,排查步骤,处理建议")
                .maxResponseTimeMs(60000).requiresSafetyWarning(true)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-005").category("设备台账")
                .question("TR-110KV-001的设备台账信息")
                .expectedIntent("DEVICE_PROFILE")
                .expectedTools(List.of("getDeviceProfile"))
                .expectedKeywords("型号,厂家,投运时间")
                .maxResponseTimeMs(8000).requiresSafetyWarning(false)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-006").category("工单查询")
                .question("查一下TR-110KV-001的历史缺陷工单")
                .expectedIntent("TICKET_QUERY")
                .expectedTools(List.of("getDefectTickets"))
                .expectedKeywords("缺陷,工单,处理状态")
                .maxResponseTimeMs(8000).requiresSafetyWarning(false)
                .build());
    }

    public List<EvalCase> getAllCases() {
        return cases;
    }

    public List<EvalCase> getCasesByCategory(String category) {
        return cases.stream().filter(c -> c.getCategory().equals(category)).toList();
    }
}
