package org.example.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.example.agent.alarm.AlarmAgent;
import org.example.agent.diagnosis.DiagnosisAgent;
import org.example.agent.knowledge.KnowledgeAgent;
import org.example.agent.router.RouterAgent;
import org.example.agent.skill.service.SkillSelector;
import org.example.agent.subagent.SubagentExecutor;
import org.example.agent.ticket.TicketAgent;
import org.example.graph.dispatcher.IntentDispatcher;
import org.example.graph.handler.*;
import org.example.graph.node.*;
import org.example.graph.subgraph.alarm.*;
import org.example.graph.subgraph.chat.ChatAgentNode;
import org.example.graph.subgraph.diagnosis.*;
import org.example.graph.subgraph.device.*;
import org.example.graph.subgraph.dynamic.*;
import org.example.graph.subgraph.knowledge.*;
import org.example.hook.HookEngine;
import org.example.memory.MemoryService;
import org.example.rag.HybridSearchService;
import org.example.rag.KnowledgeGraphService;
import org.example.rag.RerankService;
import org.example.service.RagService;
import org.example.security.ApprovalService;
import org.example.security.RbacService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
@ConditionalOnProperty(name = "powerops.graph.enabled", havingValue = "true", matchIfMissing = false)
public class PowerOpsGraphConfig {

    private static final Logger logger = LoggerFactory.getLogger(PowerOpsGraphConfig.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
    public StepHandlerRegistry stepHandlerRegistry(
            KnowledgeAgent knowledgeAgent,
            AlarmAgent alarmAgent,
            DiagnosisAgent diagnosisAgent,
            SubagentExecutor subagentExecutor,
            RagService ragService,
            ApprovalService approvalService,
            ToolCallbackProvider tools
    ) {
        StepHandlerRegistry registry = new StepHandlerRegistry();
        registry.register(new ToolStepHandler(dashScopeApiKey, tools));
        registry.register(new KnowledgeStepHandler(knowledgeAgent, dashScopeApiKey));
        registry.register(new AlarmStepHandler(alarmAgent, dashScopeApiKey));
        registry.register(new DiagnosisStepHandler(diagnosisAgent, dashScopeApiKey));
        registry.register(new RagStepHandler(ragService));
        registry.register(new SubAgentStepHandler(subagentExecutor));
        registry.register(new ApprovalStepHandler(approvalService));
        registry.register(new ChatStepHandler(dashScopeApiKey, tools));
        return registry;
    }

    @Bean
    public StateGraph powerOpsGraph(
            RouterAgent routerAgent,
            SkillSelector skillSelector,
            MemoryService memoryService,
            HookEngine hookEngine,
            ToolCallbackProvider tools,
            ChatClient.Builder chatClientBuilder,
            HybridSearchService hybridSearchService,
            RerankService rerankService,
            KnowledgeGraphService knowledgeGraphService,
            RbacService rbacService,
            SubagentExecutor subagentExecutor,
            DiagnosisAgent diagnosisAgent,
            StepHandlerRegistry handlerRegistry
    ) throws GraphStateException {

        KeyStrategyFactory stateFactory = new PowerOpsStateFactory();
        ChatClient chatClient = chatClientBuilder.build();

        ChatAgentNode chatAgentNode = new ChatAgentNode(dashScopeApiKey, tools);

        StateGraph graph = new StateGraph("power_ops_workflow", stateFactory)
                .addNode("pre_check", node_async(new PreCheckNode()))
                .addNode("context_load", node_async(new ContextLoadNode(memoryService, skillSelector)))
                .addNode("router", node_async(new RouterNode(routerAgent)))
                .addNode("knowledge_qa_query_rewrite", node_async(new QueryRewriteNode()))
                .addNode("knowledge_qa_rag_retrieve", node_async(new RagRetrieveNode(hybridSearchService)))
                .addNode("knowledge_qa_rerank", node_async(new RerankNode(rerankService)))
                .addNode("knowledge_qa_answer_generate", node_async(new AnswerGenerateNode(chatClient, knowledgeGraphService)))
                .addNode("knowledge_qa_citation_check", node_async(new CitationCheckNode()))
                .addNode("device_entity_extract", node_async(new EntityExtractNode()))
                .addNode("device_permission_check", node_async(new PermissionCheckNode(rbacService)))
                .addNode("device_tool_select", node_async(new ToolSelectNode()))
                .addNode("device_tool_execute", node_async(new ToolExecuteNode(dashScopeApiKey, tools)))
                .addNode("device_data_format", node_async(new DataFormatNode()))
                .addNode("alarm_parse", node_async(new AlarmParseNode()))
                .addNode("alarm_history", node_async(new AlarmHistoryNode(dashScopeApiKey, tools)))
                .addNode("alarm_related_device", node_async(new RelatedDeviceNode(dashScopeApiKey, tools)))
                .addNode("alarm_reasoning", node_async(new AlarmReasoningNode(chatClient)))
                .addNode("alarm_suggestion", node_async(new AlarmSuggestionNode(chatClient)))
                .addNode("diagnosis_entity_extract", node_async(new DiagnosisEntityExtractNode()))
                .addNode("evidence_parallel", node_async(new EvidenceParallelNode(subagentExecutor)))
                .addNode("diagnosis", node_async(new DiagnosisNode(diagnosisAgent, dashScopeApiKey)))
                .addNode("risk_assessment", node_async(new RiskAssessmentNode(chatClient)))
                .addNode("diagnosis_replanner", node_async(new DiagnosisReplannerNode(chatClient)))
                .addNode("action_recommend", node_async(new ActionRecommendNode(chatClient)))
                .addNode("dynamic_planner", node_async(new PlannerNode(chatClient)))
                .addNode("dynamic_executor", node_async(new DynamicExecutorNode(handlerRegistry)))
                .addNode("dynamic_replanner", node_async(new DynamicReplannerNode(chatClient)))
                .addNode("finalize_plan", node_async(new FinalizePlanNode()))
                .addNode("chat", node_async(chatAgentNode))
                .addNode("safety_review", node_async(new SafetyReviewNode(hookEngine)))
                .addNode("final_response", node_async(new FinalResponseNode()))
                .addNode("memory_save", node_async(new MemorySaveNode(memoryService)))
                .addEdge(START, "pre_check")
                .addEdge("pre_check", "context_load")
                .addEdge("context_load", "router")
                .addConditionalEdges("router", edge_async(new IntentDispatcher()),
                        Map.of(
                                "knowledge_qa", "knowledge_qa_query_rewrite",
                                "device_query", "device_entity_extract",
                                "alarm_analysis", "alarm_parse",
                                "fault_diagnosis", "diagnosis_entity_extract",
                                "dynamic_plan", "dynamic_planner",
                                "chat", "chat"
                        ))
                .addEdge("knowledge_qa_query_rewrite", "knowledge_qa_rag_retrieve")
                .addEdge("knowledge_qa_rag_retrieve", "knowledge_qa_rerank")
                .addEdge("knowledge_qa_rerank", "knowledge_qa_answer_generate")
                .addEdge("knowledge_qa_answer_generate", "knowledge_qa_citation_check")
                .addEdge("knowledge_qa_citation_check", "safety_review")
                .addEdge("device_entity_extract", "device_permission_check")
                .addEdge("device_permission_check", "device_tool_select")
                .addEdge("device_tool_select", "device_tool_execute")
                .addEdge("device_tool_execute", "device_data_format")
                .addEdge("device_data_format", "safety_review")
                .addEdge("alarm_parse", "alarm_history")
                .addEdge("alarm_history", "alarm_related_device")
                .addEdge("alarm_related_device", "alarm_reasoning")
                .addEdge("alarm_reasoning", "alarm_suggestion")
                .addEdge("alarm_suggestion", "safety_review")
                .addEdge("diagnosis_entity_extract", "evidence_parallel")
                .addEdge("evidence_parallel", "diagnosis")
                .addEdge("diagnosis", "risk_assessment")
                .addEdge("risk_assessment", "diagnosis_replanner")
                .addConditionalEdges("diagnosis_replanner", edge_async(new DiagnosisReplannerDispatcher()),
                        Map.of(
                                "evidence_parallel", "evidence_parallel",
                                "action_recommend", "action_recommend"
                        ))
                .addEdge("action_recommend", "safety_review")
                .addEdge("dynamic_planner", "dynamic_executor")
                .addEdge("dynamic_executor", "dynamic_replanner")
                .addConditionalEdges("dynamic_replanner", edge_async(new DynamicReplannerDispatcher()),
                        Map.of(
                                "dynamic_planner", "dynamic_planner",
                                "dynamic_executor", "dynamic_executor",
                                "finalize_plan", "finalize_plan"
                        ))
                .addEdge("finalize_plan", "safety_review")
                .addEdge("chat", "safety_review")
                .addEdge("safety_review", "final_response")
                .addEdge("final_response", "memory_save")
                .addEdge("memory_save", END);

        try {
            GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "power_ops_workflow");
            logger.info("\n=== PowerOps Workflow UML ===\n{}\n===", representation.content());
        } catch (Exception e) {
            logger.warn("图可视化生成失败: {}", e.getMessage());
        }

        return graph;
    }

    @Bean
    public CompiledGraph compiledPowerOpsGraph(StateGraph powerOpsGraph) throws GraphStateException {
        return powerOpsGraph.compile();
    }
}
