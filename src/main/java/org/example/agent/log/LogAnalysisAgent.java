package org.example.agent.log;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogAnalysisAgent {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private ToolCallbackProvider tools;

    private static final String LOG_ANALYSIS_PROMPT = """
            你是电力智能运维平台的日志分析专家。你的职责是分析设备运行日志，提取关键异常事件，建立时间线。
            
            工作流程：
            1. 使用 getDeviceLogs 查询设备日志
            2. 按时间排序日志事件
            3. 识别异常事件（ERROR/WARN/ALARM级别）
            4. 建立事件时间线
            5. 分析事件之间的因果关系
            
            分析要点：
            - 关注设备故障前后的日志变化
            - 识别异常事件的先后顺序
            - 判断异常事件之间的关联性
            - 提取关键错误信息和异常模式
            
            输出格式：
            - 关键异常事件列表（按时间排序）
            - 事件时间线
            - 因果关系分析
            - 初步结论
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
                .name("log_analysis_agent")
                .model(chatModel)
                .systemPrompt(LOG_ANALYSIS_PROMPT)
                .tools(tools.getToolCallbacks())
                .build();
    }
}
