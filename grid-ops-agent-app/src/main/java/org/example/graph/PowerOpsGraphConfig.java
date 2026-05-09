package org.example.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.github.resilience4j.retry.RetryRegistry;
import org.example.agent.analysis.AnalysisAgent;
import org.example.agent.diagnosis.DiagnosisAgent;
import org.example.agent.risk.RiskReviewAgent;
import org.example.agent.router.RouterAgent;
import org.example.agent.skill.service.SkillSelector;
import org.example.agent.tool_agent.ToolAgent;
import org.example.checkpoint.CheckpointService;
import org.example.graph.dispatcher.EvidenceValidationDispatcher;
import org.example.graph.dispatcher.IntentDispatcher;
import org.example.graph.handler.AnalysisStepHandler;
import org.example.graph.handler.ApprovalStepHandler;
import org.example.graph.handler.ChatStepHandler;
import org.example.graph.handler.DiagnosisStepHandler;
import org.example.graph.handler.RagStepHandler;
import org.example.graph.handler.StepHandlerRegistry;
import org.example.graph.handler.ToolStepHandler;
import org.example.graph.node.ContextLoadNode;
import org.example.graph.node.FinalResponseNode;
import org.example.graph.node.MemorySaveNode;
import org.example.graph.node.ObservedNodeAction;
import org.example.graph.node.PreCheckNode;
import org.example.graph.node.RouterNode;
import org.example.graph.node.SafetyReviewNode;
import org.example.graph.subgraph.chat.ChatAgentNode;
import org.example.graph.subgraph.diagnosis.ActionRecommendNode;
import org.example.graph.subgraph.diagnosis.AlarmRagRetrieveNode;
import org.example.graph.subgraph.diagnosis.DiagnosisNode;
import org.example.graph.subgraph.diagnosis.EntityExtractNode;
import org.example.graph.subgraph.diagnosis.EvidenceValidationNode;
import org.example.graph.subgraph.diagnosis.ExecutorNode;
import org.example.graph.subgraph.diagnosis.PlannerNode;
import org.example.graph.subgraph.diagnosis.ReplannerDispatcher;
import org.example.graph.subgraph.diagnosis.ReplannerNode;
import org.example.graph.subgraph.diagnosis.RiskAssessmentNode;
import org.example.graph.subgraph.knowledge.AnswerReviewDispatcher;
import org.example.graph.subgraph.knowledge.AnswerReviewNode;
import org.example.graph.subgraph.knowledge.CitationCheckNode;
import org.example.graph.subgraph.knowledge.QueryRewriteNode;
import org.example.graph.subgraph.knowledge.RagRetrieveNode;
import org.example.graph.subgraph.knowledge.ReactQaAgentNode;
import org.example.graph.subgraph.knowledge.RerankNode;
import org.example.graph.subgraph.knowledge.ToolExecuteNode;
import org.example.graph.validation.DiagnosisValidator;
import org.example.graph.validation.EvidenceQualityEvaluator;
import org.example.graph.validation.InputValidator;
import org.example.graph.validation.PlanValidator;
import org.example.graph.validation.ToolResultValidator;
import org.example.hook.HookEngine;
import org.example.memory.MemoryService;
import org.example.observability.ObservabilityService;
import org.example.rag.HybridSearchService;
import org.example.rag.KnowledgeGraphService;
import org.example.rag.RerankService;
import org.example.security.ApprovalService;
import org.example.security.RbacService;
import org.example.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class PowerOpsGraphConfig {

    private static final Logger logger = LoggerFactory.getLogger(PowerOpsGraphConfig.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
    public StepHandlerRegistry stepHandlerRegistry(
            ToolAgent toolAgent,
            AnalysisAgent analysisAgent,
            DiagnosisAgent diagnosisAgent,
            RagService ragService,
            ApprovalService approvalService,
            ToolCallbackProvider tools
    ) {
        StepHandlerRegistry registry = new StepHandlerRegistry();
        registry.register(new ToolStepHandler(toolAgent, dashScopeApiKey));
        registry.register(new AnalysisStepHandler(analysisAgent, dashScopeApiKey));
        registry.register(new DiagnosisStepHandler(diagnosisAgent, dashScopeApiKey));
        registry.register(new RagStepHandler(ragService));
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
            ToolAgent toolAgent,
            AnalysisAgent analysisAgent,
            DiagnosisAgent diagnosisAgent,
            RiskReviewAgent riskReviewAgent,
            ToolCallbackProvider tools,
            ChatClient.Builder chatClientBuilder,
            HybridSearchService hybridSearchService,
            RerankService rerankService,
            KnowledgeGraphService knowledgeGraphService,
            RbacService rbacService,
            StepHandlerRegistry handlerRegistry,
            InputValidator inputValidator,
            PlanValidator planValidator,
            ToolResultValidator toolResultValidator,
            EvidenceQualityEvaluator evidenceQualityEvaluator,
            DiagnosisValidator diagnosisValidator,
            RetryRegistry retryRegistry,
            ObservabilityService observabilityService,
            CheckpointService checkpointService
    ) throws GraphStateException {

        KeyStrategyFactory stateFactory = new PowerOpsStateFactory();
        ChatClient chatClient = chatClientBuilder.build();
        ChatAgentNode chatAgentNode = new ChatAgentNode(dashScopeApiKey, tools, hybridSearchService);

        StateGraph graph = new StateGraph("power_ops_workflow", stateFactory)
                .addNode("pre_check", node_async(observed("pre_check",
                        new PreCheckNode(inputValidator), observabilityService, checkpointService)))
                .addNode("context_load", node_async(observed("context_load",
                        new ContextLoadNode(memoryService, skillSelector), observabilityService, checkpointService)))
                .addNode("router", node_async(observed("router",
                        new RouterNode(routerAgent), observabilityService, checkpointService)))

                .addNode("query_rewrite", node_async(observed("query_rewrite",
                        new QueryRewriteNode(rbacService, chatClient), observabilityService, checkpointService)))
                .addNode("rag_retrieve", node_async(observed("rag_retrieve",
                        new RagRetrieveNode(hybridSearchService), observabilityService, checkpointService)))
                .addNode("tool_execute", node_async(observed("tool_execute",
                        new ToolExecuteNode(toolAgent, dashScopeApiKey), observabilityService, checkpointService)))
                .addNode("rerank", node_async(observed("rerank",
                        new RerankNode(rerankService), observabilityService, checkpointService)))
                .addNode("react_qa_agent", node_async(observed("react_qa_agent",
                        new ReactQaAgentNode(dashScopeApiKey, tools, knowledgeGraphService), observabilityService, checkpointService)))
                .addNode("answer_review", node_async(observed("answer_review",
                        new AnswerReviewNode(chatClient), observabilityService, checkpointService)))
                .addNode("citation_check", node_async(observed("citation_check",
                        new CitationCheckNode(), observabilityService, checkpointService)))

                .addNode("entity_extract", node_async(observed("entity_extract",
                        new EntityExtractNode(chatClient), observabilityService, checkpointService)))
                .addNode("alarm_rag_retrieve", node_async(observed("alarm_rag_retrieve",
                        new AlarmRagRetrieveNode(hybridSearchService), observabilityService, checkpointService)))
                .addNode("planner", node_async(observed("planner",
                        new PlannerNode(chatClient, planValidator, tools), observabilityService, checkpointService)))
                .addNode("executor", node_async(observed("executor",
                        new ExecutorNode(tools, retryRegistry, toolResultValidator, observabilityService),
                        observabilityService, checkpointService)))
                .addNode("evidence_validation", node_async(observed("evidence_validation",
                        new EvidenceValidationNode(evidenceQualityEvaluator), observabilityService, checkpointService)))
                .addNode("diagnosis", node_async(observed("diagnosis",
                        new DiagnosisNode(diagnosisAgent, dashScopeApiKey, diagnosisValidator),
                        observabilityService, checkpointService)))
                .addNode("risk_assessment", node_async(observed("risk_assessment",
                        new RiskAssessmentNode(riskReviewAgent, dashScopeApiKey), observabilityService, checkpointService)))
                .addNode("replanner", node_async(observed("replanner",
                        new ReplannerNode(chatClient), observabilityService, checkpointService)))
                .addNode("action_recommend", node_async(observed("action_recommend",
                        new ActionRecommendNode(), observabilityService, checkpointService)))

                .addNode("chat", node_async(observed("chat",
                        chatAgentNode, observabilityService, checkpointService)))
                .addNode("safety_review", node_async(observed("safety_review",
                        new SafetyReviewNode(hookEngine), observabilityService, checkpointService)))
                .addNode("final_response", node_async(observed("final_response",
                        new FinalResponseNode(), observabilityService, checkpointService)))
                .addNode("memory_save", node_async(observed("memory_save",
                        new MemorySaveNode(memoryService), observabilityService, checkpointService)))

                .addEdge(START, "pre_check")
                .addEdge("pre_check", "context_load")
                .addEdge("context_load", "router")

                .addConditionalEdges("router", edge_async(new IntentDispatcher()),
                        Map.of(
                                "knowledge_qa", "query_rewrite",
                                "diagnosis", "entity_extract",
                                "chat", "chat"
                        ))

                .addEdge("query_rewrite", "rag_retrieve")
                .addEdge("rag_retrieve", "tool_execute")
                .addEdge("tool_execute", "rerank")
                .addEdge("rerank", "react_qa_agent")
                .addEdge("react_qa_agent", "answer_review")
                .addConditionalEdges("answer_review", edge_async(new AnswerReviewDispatcher()),
                        Map.of(
                                "rag_retrieve", "rag_retrieve",
                                "citation_check", "citation_check"
                        ))
                .addEdge("citation_check", "safety_review")

                .addEdge("entity_extract", "alarm_rag_retrieve")
                .addEdge("alarm_rag_retrieve", "planner")
                .addEdge("planner", "executor")
                .addEdge("executor", "evidence_validation")
                .addConditionalEdges("evidence_validation", edge_async(new EvidenceValidationDispatcher()),
                        Map.of(
                                "diagnosis", "diagnosis",
                                "replanner", "replanner",
                                "action_recommend", "action_recommend"
                        ))
                .addEdge("diagnosis", "risk_assessment")
                .addEdge("risk_assessment", "replanner")
                .addConditionalEdges("replanner", edge_async(new ReplannerDispatcher()),
                        Map.of(
                                "executor", "executor",
                                "action_recommend", "action_recommend"
                        ))
                .addEdge("action_recommend", "safety_review")

                .addEdge("chat", "safety_review")
                .addEdge("safety_review", "final_response")
                .addEdge("final_response", "memory_save")
                .addEdge("memory_save", END);

        try {
            GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "power_ops_workflow");
            logger.info("\n=== PowerOps Workflow UML ===\n{}\n===", representation.content());
        } catch (Exception e) {
            logger.warn("Graph visualization generation failed: {}", e.getMessage());
        }

        return graph;
    }

    @Bean
    public CompiledGraph compiledPowerOpsGraph(StateGraph powerOpsGraph) throws GraphStateException {
        return powerOpsGraph.compile();
    }

    private NodeAction observed(String nodeName, NodeAction nodeAction,
                                ObservabilityService observabilityService,
                                CheckpointService checkpointService) {
        return new ObservedNodeAction(nodeName, nodeAction, observabilityService, checkpointService);
    }
}
