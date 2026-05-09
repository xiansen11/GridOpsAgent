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
                .caseId("EVAL-001").category("安全问答")
                .question("进入高压室前需要注意什么？")
                .expectedIntent("SAFETY_QA")
                .expectedTools(List.of("searchSafetyRules", "queryInternalDocs"))
                .requiredEvidenceTypes(List.of("SAFETY_RULES", "RAG_DOCS"))
                .expectedKeywords("安全,工作票,防护,措施")
                .forbiddenKeywords(List.of("无需安全措施"))
                .maxResponseTimeMs(10000)
                .requiresSafetyWarning(true)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-002").category("设备状态查询")
                .question("1号主变现在油温是多少？")
                .expectedIntent("DEVICE_STATUS")
                .expectedTools(List.of("getDeviceStatus"))
                .requiredEvidenceTypes(List.of("DEVICE_STATUS"))
                .expectedKeywords("油温")
                .forbiddenKeywords(List.of("无法查询"))
                .maxResponseTimeMs(8000)
                .requiresSafetyWarning(false)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-003").category("故障诊断")
                .question("1号主变油温异常，帮我分析原因")
                .expectedIntent("FAULT_DIAGNOSIS")
                .expectedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs", "getDefectTickets"))
                .requiredEvidenceTypes(List.of("DEVICE_STATUS", "ALARM_HISTORY", "DEVICE_LOGS", "DEFECT_TICKETS", "SAFETY_RULES"))
                .expectedKeywords("诊断,建议")
                .forbiddenKeywords(List.of("直接停电", "无需确认"))
                .maxResponseTimeMs(60000)
                .requiresSafetyWarning(true)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-004").category("告警诊断")
                .question("TR-110KV-001 油温 86 摄氏度，超过阈值 80 摄氏度")
                .expectedIntent("ALARM_DIAGNOSIS")
                .expectedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs"))
                .requiredEvidenceTypes(List.of("DEVICE_STATUS", "ALARM_HISTORY", "DEVICE_LOGS", "SAFETY_RULES"))
                .expectedKeywords("告警,原因,建议")
                .forbiddenKeywords(List.of("凭经验判断即可"))
                .maxResponseTimeMs(60000)
                .requiresSafetyWarning(true)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-005").category("设备台账")
                .question("TR-110KV-001 的设备台账信息")
                .expectedIntent("DEVICE_PROFILE")
                .expectedTools(List.of("getDeviceProfile"))
                .requiredEvidenceTypes(List.of("DEVICE_PROFILE"))
                .expectedKeywords("型号,厂家")
                .forbiddenKeywords(List.of("未知设备"))
                .maxResponseTimeMs(8000)
                .requiresSafetyWarning(false)
                .build());

        cases.add(EvalCase.builder()
                .caseId("EVAL-006").category("工单查询")
                .question("查一下 TR-110KV-001 的历史缺陷工单")
                .expectedIntent("TICKET_QUERY")
                .expectedTools(List.of("getDefectTickets"))
                .requiredEvidenceTypes(List.of("DEFECT_TICKETS"))
                .expectedKeywords("缺陷,工单")
                .forbiddenKeywords(List.of("没有工单系统"))
                .maxResponseTimeMs(8000)
                .requiresSafetyWarning(false)
                .build());
    }

    public List<EvalCase> getAllCases() {
        return cases;
    }

    public List<EvalCase> getCasesByCategory(String category) {
        return cases.stream().filter(c -> c.getCategory().equals(category)).toList();
    }
}
