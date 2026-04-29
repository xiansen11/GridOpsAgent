package org.example.agent.router;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RouterAgent {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    private static final String ROUTER_PROMPT = """
            你是电力智能运维平台的意图识别路由器。你需要分析用户输入，判断其意图类型，并返回路由信息。
            
            意图类型列表：
            1. SAFETY_QA - 安规问答：用户询问电力安全规程、操作规程、安全措施等
            2. DEVICE_STATUS - 设备状态查询：用户询问设备实时运行状态、油温、负荷等
            3. ALARM_QUERY - 告警查询：用户询问历史告警记录、告警统计等
            4. LOG_ANALYSIS - 日志分析：用户要求分析设备运行日志、查找异常事件
            5. TICKET_QUERY - 工单查询：用户查询历史缺陷工单、维修记录等
            6. DEVICE_PROFILE - 设备台账：用户查询设备型号、厂家、投运时间等基本信息
            7. FAULT_DIAGNOSIS - 故障诊断：用户描述设备故障，要求分析原因和给出建议
            8. ALARM_DIAGNOSIS - 告警诊断：用户提交告警事件，要求自动诊断
            9. GENERAL_CHAT - 通用对话：其他一般性问答
            
            请分析以下用户输入，返回JSON格式：
            {"intent": "意图类型", "confidence": 0.95, "deviceId": "提取的设备编号(如有)", "keywords": ["关键词1", "关键词2"]}
            
            只返回JSON，不要其他内容。
            
            用户输入：{question}
            """;

    public String route(String question) {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.1)
                        .withMaxToken(200)
                        .withTopP(0.5)
                        .build())
                .build();

        PromptTemplate promptTemplate = new PromptTemplate(ROUTER_PROMPT);
        var prompt = promptTemplate.create(Map.of("question", question));
        var response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}
