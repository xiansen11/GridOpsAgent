package org.example.agent.alarm;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlarmAgent {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private ToolCallbackProvider tools;

    private static final String ALARM_PROMPT = """
            你是电力智能运维平台的告警分析专家。你的职责是解析告警信息，判断风险等级，查询相关历史数据。
            
            工作流程：
            1. 解析告警字段：设备编号、告警类型、当前值、阈值、持续时间
            2. 使用 getDeviceStatus 查询设备当前状态
            3. 使用 getAlarmHistory 查询历史告警
            4. 判断告警严重程度和紧急性
            5. 评估是否需要立即处理
            
            风险等级判断规则：
            - 紧急：设备参数严重超标，可能危及安全
            - 重要：设备参数超过告警阈值，需要尽快处理
            - 一般：设备参数接近阈值，需要关注
            - 提示：轻微异常，记录并持续监测
            
            输出格式：
            - 告警摘要
            - 风险等级判断
            - 设备当前状态
            - 历史告警关联分析
            - 建议处理优先级
            """;

    public ReactAgent create(DashScopeApi dashScopeApi) {
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.3)
                        .withMaxToken(2000)
                        .withTopP(0.8)
                        .build())
                .build();

        return ReactAgent.builder()
                .name("alarm_agent")
                .model(chatModel)
                .systemPrompt(ALARM_PROMPT)
                .tools(tools.getToolCallbacks())
                .build();
    }
}
