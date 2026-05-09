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
            
            意图类型列表（仅3种）：
            1. KNOWLEDGE_QA - 知识问答与设备查询：用户询问电力安全规程、操作规程、设备实时运行状态、设备台账信息、安规查询等
            2. DIAGNOSIS - 诊断分析：用户描述设备故障、提交告警事件要求诊断、查询历史告警、要求复杂任务分析等
            3. CHAT - 通用对话：日志分析、工单查询、其他一般性问答
            
            判断规则：
            - 涉及"查询设备状态/台账/安规/规程" → KNOWLEDGE_QA
            - 涉及"故障诊断/告警诊断/告警分析/复杂任务" → DIAGNOSIS
            - 涉及"日志分析/工单查询/闲聊/其他" → CHAT
            
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
