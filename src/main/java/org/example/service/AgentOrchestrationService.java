package org.example.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.analysis.AnalysisAgent;
import org.example.agent.diagnosis.DiagnosisAgent;
import org.example.agent.risk.RiskReviewAgent;
import org.example.agent.router.RouterAgent;
import org.example.agent.skill.model.Skill;
import org.example.agent.skill.service.SkillSelector;
import org.example.agent.tool_agent.ToolAgent;
import org.example.checkpoint.CheckpointService;
import org.example.hook.HookContext;
import org.example.hook.HookEngine;
import org.example.hook.HookResult;
import org.example.memory.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrationService.class);

    @Autowired
    private RouterAgent routerAgent;

    @Autowired
    private ToolAgent toolAgent;

    @Autowired
    private AnalysisAgent analysisAgent;

    @Autowired
    private DiagnosisAgent diagnosisAgent;

    @Autowired
    private RiskReviewAgent riskReviewAgent;

    @Autowired
    private ToolCallbackProvider tools;

    @Autowired
    private HookEngine hookEngine;

    @Autowired
    private SkillSelector skillSelector;

    @Autowired
    private CheckpointService checkpointService;

    @Autowired
    private MemoryService memoryService;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String routeIntent(String question) {
        try {
            String routeResult = routerAgent.route(question);
            JsonNode node = objectMapper.readTree(routeResult);
            return node.has("intent") ? node.get("intent").asText() : "CHAT";
        } catch (Exception e) {
            logger.warn("意图识别失败，降级为通用对话: {}", e.getMessage());
            return "CHAT";
        }
    }

    public String handleChat(String question, List<Map<String, String>> history, String sessionId) {
        String taskId = "TASK-" + System.currentTimeMillis();

        HookContext preRouteCtx = HookContext.builder()
                .sessionId(sessionId)
                .taskId(taskId)
                .input(question)
                .params(new HashMap<>())
                .build();
        HookResult preRouteResult = hookEngine.executeHooks("PRE_ROUTE", preRouteCtx);
        if (!preRouteResult.isProceed()) {
            return "请求被拦截: " + preRouteResult.getMessage();
        }
        question = preRouteCtx.getInput();

        String intent = routeIntent(question);
        logger.info("意图识别结果: intent={}", intent);

        HookContext postRouteCtx = HookContext.builder()
                .sessionId(sessionId)
                .taskId(taskId)
                .agentName("router")
                .input(question)
                .output(intent)
                .params(new HashMap<>())
                .build();
        hookEngine.executeHooks("POST_ROUTE", postRouteCtx);
        intent = postRouteCtx.getOutput();

        checkpointService.saveCheckpoint(taskId, sessionId, "ROUTED",
                Map.of("intent", intent, "question", question));

        Skill selectedSkill = skillSelector.selectByIntent(intent).orElse(null);
        if (selectedSkill != null) {
            logger.info("匹配到Skill: {}", selectedSkill.getName());
            checkpointService.saveCheckpoint(taskId, sessionId, "SKILL_SELECTED",
                    Map.of("skillId", selectedSkill.getSkillId(), "skillName", selectedSkill.getName()));
        }

        String memoryContext = memoryService.buildContextForAgent(sessionId, taskId, "default_user");

        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();

        try {
            String result;
            switch (intent) {
                case "KNOWLEDGE_QA" -> {
                    result = handleSimpleQuery(question, history, dashScopeApi, selectedSkill, memoryContext, sessionId, taskId);
                }
                case "DIAGNOSIS" -> {
                    result = handleDiagnosis(question, dashScopeApi, selectedSkill, sessionId, taskId);
                }
                default -> {
                    result = handleSimpleQuery(question, history, dashScopeApi, selectedSkill, memoryContext, sessionId, taskId);
                }
            }

            HookContext postDiagnosisCtx = HookContext.builder()
                    .sessionId(sessionId)
                    .taskId(taskId)
                    .agentName("orchestrator")
                    .input(question)
                    .output(result)
                    .params(new HashMap<>())
                    .build();
            hookEngine.executeHooks("POST_DIAGNOSIS", postDiagnosisCtx);
            result = postDiagnosisCtx.getOutput();

            checkpointService.saveCheckpoint(taskId, sessionId, "COMPLETED",
                    Map.of("resultLength", String.valueOf(result.length())));

            memoryService.saveToSession(sessionId, "lastUserMessage", question);
            memoryService.saveToSession(sessionId, "lastAssistantMessage", result);

            return result;
        } catch (Exception e) {
            logger.error("Agent编排执行失败: intent={}", intent, e);
            checkpointService.saveCheckpoint(taskId, sessionId, "FAILED",
                    Map.of("error", e.getMessage()));
            return "抱歉，处理您的问题时出现异常：" + e.getMessage();
        }
    }

    private String handleSimpleQuery(String question, List<Map<String, String>> history,
                                      DashScopeApi dashScopeApi, Skill skill,
                                      String memoryContext, String sessionId, String taskId) {
        try {
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是电力智能运维平台的智能助手，专门面向电力巡检、监控告警处理、设备故障排障和现场知识查询场景。\n");
            systemPrompt.append("你可以帮助运维人员完成安规问答、设备状态查询、告警查询、设备台账查询、知识库检索等任务。\n");
            systemPrompt.append("重要安全规则：涉及安全操作时必须提示遵守现场规程；严禁编造数据；高风险建议必须标注并建议人工确认。\n\n");

            if (skill != null && skill.getPromptTemplate() != null) {
                systemPrompt.append("--- 当前业务场景指导 ---\n");
                systemPrompt.append(skill.getPromptTemplate()).append("\n\n");
            }

            if (memoryContext != null && !memoryContext.isEmpty()) {
                systemPrompt.append("--- 上下文记忆 ---\n");
                systemPrompt.append(memoryContext).append("\n\n");
            }

            if (!history.isEmpty()) {
                systemPrompt.append("--- 对话历史 ---\n");
                for (Map<String, String> msg : history) {
                    if ("user".equals(msg.get("role"))) {
                        systemPrompt.append("用户: ").append(msg.get("content")).append("\n");
                    } else if ("assistant".equals(msg.get("role"))) {
                        systemPrompt.append("助手: ").append(msg.get("content")).append("\n");
                    }
                }
                systemPrompt.append("--- 对话历史结束 ---\n\n");
            }

            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.7)
                            .withMaxToken(2000)
                            .withTopP(0.9)
                            .build())
                    .build();

            ReactAgent agent = ReactAgent.builder()
                    .name("chat_agent")
                    .model(chatModel)
                    .systemPrompt(systemPrompt.toString())
                    .tools(tools.getToolCallbacks())
                    .build();

            var response = agent.call(question);
            return response.getText();
        } catch (Exception e) {
            logger.error("简单查询处理失败", e);
            return "处理您的问题时出现异常：" + e.getMessage();
        }
    }

    private String handleDiagnosis(String question, DashScopeApi dashScopeApi,
                                    Skill skill, String sessionId, String taskId) {
        try {
            checkpointService.saveCheckpoint(taskId, sessionId, "ANALYSIS_START", new HashMap<>());

            String evidence = analysisAgent.create(dashScopeApi).call(question).getText();

            checkpointService.saveCheckpoint(taskId, sessionId, "ANALYSIS_DONE",
                    Map.of("evidenceLength", String.valueOf(evidence.length())));

            String skillPrompt = "";
            if (skill != null && skill.getPromptTemplate() != null) {
                skillPrompt = "\n\n--- 业务场景指导 ---\n" + skill.getPromptTemplate();
            }

            String diagnosisInput = question + "\n\n--- 收集的证据 ---\n" + evidence + skillPrompt;

            HookContext preDiagCtx = HookContext.builder()
                    .sessionId(sessionId)
                    .taskId(taskId)
                    .agentName("diagnosis")
                    .input(question)
                    .params(new HashMap<>())
                    .build();
            hookEngine.executeHooks("PRE_DIAGNOSIS", preDiagCtx);

            String diagnosisResult = diagnosisAgent.create(dashScopeApi).call(diagnosisInput).getText();

            checkpointService.saveCheckpoint(taskId, sessionId, "DIAGNOSIS_GENERATED",
                    Map.of("resultLength", String.valueOf(diagnosisResult.length())));

            String riskInput = "原始问题: " + question + "\n\n诊断结果:\n" + diagnosisResult;
            String riskResult = riskReviewAgent.create(dashScopeApi).call(riskInput).getText();

            return diagnosisResult + "\n\n--- 风险评估与行动建议 ---\n" + riskResult;
        } catch (Exception e) {
            logger.error("诊断流程执行失败", e);
            return "诊断流程执行异常：" + e.getMessage();
        }
    }

    public String resumeFromCheckpoint(String taskId) {
        try {
            Map<String, Object> checkpoint = checkpointService.restoreFromCheckpoint(taskId);
            if (checkpoint == null) {
                return "未找到任务检查点: " + taskId;
            }

            String agentState = (String) checkpoint.get("agentState");
            String question = (String) checkpoint.get("question");
            String sessionId = (String) checkpoint.get("sessionId");

            if (question == null) {
                return "检查点数据不完整，无法恢复";
            }

            logger.info("从检查点恢复任务: taskId={}, state={}", taskId, agentState);
            return handleChat(question, List.of(), sessionId);
        } catch (Exception e) {
            logger.error("断点恢复失败: taskId={}", taskId, e);
            return "断点恢复失败: " + e.getMessage();
        }
    }
}
