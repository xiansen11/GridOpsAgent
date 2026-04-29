package org.example.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiOpsService {

    private static final Logger logger = LoggerFactory.getLogger(AiOpsService.class);

    @Autowired
    private ToolCallbackProvider tools;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    private static final String DIAGNOSIS_PROMPT = """
            你是电力智能运维平台的故障诊断专家。你的职责是根据告警信息进行诊断分析。
            
            工作流程：
            1. 解析告警信息，识别告警类型、严重程度、涉及设备
            2. 调用工具查询设备状态、历史告警、设备日志、缺陷工单等
            3. 分析数据，找出可能原因
            4. 制定排查步骤和处理建议
            5. 提示安全风险
            
            你必须输出结构化的诊断报告，包含以下8项内容：
            1. 告警摘要
            2. 初步判断
            3. 分析依据
            4. 可能原因（按可能性排序）
            5. 排查步骤
            6. 处理建议
            7. 安全风险提示
            8. 是否建议派单
            
            重要安全规则：
            - 高风险操作（停电、降负荷、紧急派单）必须标注⚠️并建议人工确认
            - 严禁编造数据，只能引用工具返回的真实内容
            - 涉及安全操作时，必须提示遵守现场规程
            """;

    public DashScopeApi createDashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();
    }

    public ReactAgent createAlarmDiagnosisAgent(DashScopeApi dashScopeApi) {
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.3)
                        .withMaxToken(4000)
                        .withTopP(0.8)
                        .build())
                .build();

        ToolCallback[] toolCallbacks = tools.getToolCallbacks();

        return ReactAgent.builder()
                .name("power_diagnosis_agent")
                .model(chatModel)
                .systemPrompt(DIAGNOSIS_PROMPT)
                .tools(toolCallbacks)
                .build();
    }
}
