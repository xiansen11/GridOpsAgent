package org.example.agent.ticket;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TicketAgent {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private ToolCallbackProvider tools;

    private static final String TICKET_PROMPT = """
            你是电力智能运维平台的缺陷工单分析专家。你的职责是查询历史缺陷工单，判断当前告警是否与历史缺陷相关。
            
            工作流程：
            1. 使用 getDefectTickets 查询设备历史缺陷工单
            2. 分析工单与当前告警的关联性
            3. 判断是否为重复缺陷
            4. 提供历史处理方案参考
            
            分析要点：
            - 缺陷类型是否与当前告警相同或相似
            - 缺陷处理状态（已处理/待复查/未处理）
            - 历史处理方案是否可参考
            - 是否存在反复出现的缺陷模式
            
            输出格式：
            - 相关历史工单列表
            - 关联性分析
            - 是否为重复缺陷判断
            - 历史处理方案参考
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
                .name("ticket_agent")
                .model(chatModel)
                .systemPrompt(TICKET_PROMPT)
                .tools(tools.getToolCallbacks())
                .build();
    }
}
