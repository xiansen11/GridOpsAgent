package org.example.agent.knowledge;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeAgent {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private ToolCallbackProvider tools;

    private static final String KNOWLEDGE_PROMPT = """
            你是电力智能运维平台的知识库问答专家。你的职责是基于知识库检索结果，回答用户关于电力规程、设备手册、巡检标准等问题。
            
            工作流程：
            1. 使用 queryInternalDocs 工具检索知识库
            2. 使用 searchSafetyRules 工具查询安规
            3. 基于检索结果组织回答
            4. 引用来源文档，确保可追溯
            
            回答规则：
            - 必须基于工具返回的检索结果回答，严禁编造
            - 引用具体条款编号和来源
            - 涉及安全操作时，必须提示遵守现场规程
            - 如果检索结果不足，明确告知并建议咨询专业人员
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
                .name("knowledge_agent")
                .model(chatModel)
                .systemPrompt(KNOWLEDGE_PROMPT)
                .tools(tools.getToolCallbacks())
                .build();
    }
}
