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

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ToolCallbackProvider tools;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    public DashScopeApi createDashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();
    }

    public DashScopeChatModel createChatModel(DashScopeApi dashScopeApi, double temperature, int maxToken, double topP) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(temperature)
                        .withMaxToken(maxToken)
                        .withTopP(topP)
                        .build())
                .build();
    }

    public DashScopeChatModel createStandardChatModel(DashScopeApi dashScopeApi) {
        return createChatModel(dashScopeApi, 0.7, 2000, 0.9);
    }

    public String buildSystemPrompt(List<Map<String, String>> history) {
        StringBuilder systemPromptBuilder = new StringBuilder();

        systemPromptBuilder.append("你是电力智能运维平台的智能助手，专门面向电力巡检、监控告警处理、设备故障排障和现场知识查询场景。\n");
        systemPromptBuilder.append("你可以帮助运维人员完成以下任务：\n");
        systemPromptBuilder.append("1. 电力安规问答：查询电力安全工作规程、变电运维规程等\n");
        systemPromptBuilder.append("2. 设备状态查询：查询变压器、开关柜等设备的实时运行状态（油温、负荷、冷却器状态等）\n");
        systemPromptBuilder.append("3. 告警分析：查询设备历史告警，分析告警原因\n");
        systemPromptBuilder.append("4. 设备日志分析：查询设备运行日志，分析故障前后状态变化\n");
        systemPromptBuilder.append("5. 历史工单查询：查询设备历史缺陷工单，判断是否重复缺陷\n");
        systemPromptBuilder.append("6. 设备台账查询：查询设备型号、厂家、投运时间等基本信息\n");
        systemPromptBuilder.append("7. 知识库检索：检索电力规程、设备手册、巡检标准等知识文档\n\n");
        systemPromptBuilder.append("工具使用指南：\n");
        systemPromptBuilder.append("- 查询设备实时状态时，使用 getDeviceStatus 工具\n");
        systemPromptBuilder.append("- 查询历史告警时，使用 getAlarmHistory 工具\n");
        systemPromptBuilder.append("- 查询设备日志时，使用 getDeviceLogs 工具\n");
        systemPromptBuilder.append("- 查询缺陷工单时，使用 getDefectTickets 工具\n");
        systemPromptBuilder.append("- 查询安规时，使用 searchSafetyRules 工具\n");
        systemPromptBuilder.append("- 查询设备台账时，使用 getDeviceProfile 工具\n");
        systemPromptBuilder.append("- 查询知识文档时，使用 queryInternalDocs 工具\n");
        systemPromptBuilder.append("- 获取当前时间时，使用 getCurrentDateTime 工具\n\n");
        systemPromptBuilder.append("重要安全规则：\n");
        systemPromptBuilder.append("- 涉及安全操作时，必须提示遵守现场规程\n");
        systemPromptBuilder.append("- 严禁编造数据，只能引用工具返回的真实内容\n");
        systemPromptBuilder.append("- 高风险建议（如停电、降负荷、紧急派单）必须明确标注并建议人工确认\n\n");

        if (!history.isEmpty()) {
            systemPromptBuilder.append("--- 对话历史 ---\n");
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    systemPromptBuilder.append("用户: ").append(content).append("\n");
                } else if ("assistant".equals(role)) {
                    systemPromptBuilder.append("助手: ").append(content).append("\n");
                }
            }
            systemPromptBuilder.append("--- 对话历史结束 ---\n\n");
        }

        systemPromptBuilder.append("请基于以上对话历史，回答用户的新问题。");

        return systemPromptBuilder.toString();
    }

    public ToolCallback[] getToolCallbacks() {
        return tools.getToolCallbacks();
    }

    public void logAvailableTools() {
        ToolCallback[] toolCallbacks = tools.getToolCallbacks();
        logger.info("可用工具列表:");
        for (ToolCallback toolCallback : toolCallbacks) {
            logger.info(">>> {}", toolCallback.getToolDefinition().name());
        }
    }

    public ReactAgent createReactAgent(DashScopeChatModel chatModel, String systemPrompt) {
        return ReactAgent.builder()
                .name("power_aiops_assistant")
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .tools(getToolCallbacks())
                .build();
    }

    public String executeChat(ReactAgent agent, String question) throws Exception {
        logger.info("执行 ReactAgent.call() - 自动处理工具调用");
        var response = agent.call(question);
        String answer = response.getText();
        logger.info("ReactAgent 对话完成，答案长度: {}", answer.length());
        return answer;
    }
}
