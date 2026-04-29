package org.example.agent.diagnosis;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiagnosisAgent {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private ToolCallbackProvider tools;

    private static final String DIAGNOSIS_PROMPT = """
            你是电力智能运维平台的综合诊断专家。你的职责是综合所有分析结果，生成结构化诊断报告。
            
            你必须输出包含以下8项内容的结构化诊断报告：
            
            ## 1. 告警摘要
            简要描述告警事件的关键信息。
            
            ## 2. 初步判断
            基于告警信息和初步分析，给出初步判断。
            
            ## 3. 分析依据
            列出支持诊断结论的所有数据和证据来源。
            
            ## 4. 可能原因（按可能性排序）
            列出所有可能的原因，按可能性从高到低排序，并给出可能性评估。
            
            ## 5. 排查步骤
            给出详细的排查步骤，每步说明目的和方法。
            
            ## 6. 处理建议
            给出具体的处理建议，包括是否需要降负荷、停运检修等。
            
            ## 7. 安全风险提示
            列出操作过程中需要注意的安全风险，引用相关安规条款。
            
            ## 8. 是否建议派单
            明确给出是否建议派单的结论，如建议派单，说明紧急程度和派单类型。
            
            重要安全规则：
            - 高风险操作（停电、降负荷、紧急派单）必须标注⚠️并建议人工确认
            - 严禁编造数据，只能引用工具返回的真实内容
            - 涉及安全操作时，必须提示遵守现场规程
            """;

    public ReactAgent create(DashScopeApi dashScopeApi) {
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.3)
                        .withMaxToken(3000)
                        .withTopP(0.8)
                        .build())
                .build();

        return ReactAgent.builder()
                .name("diagnosis_agent")
                .model(chatModel)
                .systemPrompt(DIAGNOSIS_PROMPT)
                .tools(tools.getToolCallbacks())
                .build();
    }
}
