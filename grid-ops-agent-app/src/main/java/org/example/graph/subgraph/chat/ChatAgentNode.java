package org.example.graph.subgraph.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.rag.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;
import java.util.Map;

public class ChatAgentNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ChatAgentNode.class);

    private final String dashScopeApiKey;
    private final ToolCallbackProvider tools;
    private final HybridSearchService hybridSearchService;

    public ChatAgentNode(String dashScopeApiKey, ToolCallbackProvider tools, HybridSearchService hybridSearchService) {
        this.dashScopeApiKey = dashScopeApiKey;
        this.tools = tools;
        this.hybridSearchService = hybridSearchService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        String skillContext = state.value("skill_context").map(Object::toString).orElse("");
        String memoryContext = state.value("memory_context").map(Object::toString).orElse("");

        logger.info("ChatAgentNode: 处理通用对话(RAG增强), input={}", input.substring(0, Math.min(50, input.length())));

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是电力智能运维平台的智能助手，专门面向电力巡检、监控告警处理、设备故障排障和现场知识查询场景。\n");
        systemPrompt.append("你可以帮助运维人员完成安规问答、设备状态查询、告警查询、设备台账查询、知识库检索等任务。\n");
        systemPrompt.append("重要安全规则：涉及安全操作时必须提示遵守现场规程；严禁编造数据；高风险建议必须标注并建议人工确认。\n\n");

        if (hybridSearchService != null) {
            try {
                List<HybridSearchService.HybridSearchResult> ragResults = hybridSearchService.hybridSearch(input, 3);
                if (!ragResults.isEmpty()) {
                    systemPrompt.append("--- 知识库检索结果 ---\n");
                    for (HybridSearchService.HybridSearchResult r : ragResults) {
                        systemPrompt.append(r.getContent()).append("\n\n");
                    }
                    logger.info("ChatAgentNode: RAG检索增强，获取{}条结果", ragResults.size());
                }
            } catch (Exception e) {
                logger.warn("ChatAgentNode: RAG检索失败，继续无RAG模式, error={}", e.getMessage());
            }
        }

        if (!skillContext.isEmpty()) {
            systemPrompt.append("--- 当前业务场景指导 ---\n").append(skillContext).append("\n\n");
        }
        if (!memoryContext.isEmpty()) {
            systemPrompt.append("--- 上下文记忆 ---\n").append(memoryContext).append("\n\n");
        }

        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.7).withMaxToken(2000).withTopP(0.9).build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("power_chat_agent")
                .model(chatModel)
                .systemPrompt(systemPrompt.toString())
                .tools(tools.getToolCallbacks())
                .build();

        String result = agent.call(input).getText();
        return Map.of("final_response", result);
    }
}
