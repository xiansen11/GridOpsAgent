package org.example.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.example.agent.analysis.AnalysisAgent;
import org.example.agent.diagnosis.DiagnosisAgent;
import org.example.agent.risk.RiskReviewAgent;
import org.example.agent.router.RouterAgent;
import org.example.agent.skill.service.SkillSelector;
import org.example.agent.tool_agent.ToolAgent;
import org.example.graph.dispatcher.IntentDispatcher;
import org.example.graph.handler.*;
import org.example.graph.node.*;
import org.example.graph.subgraph.chat.ChatAgentNode;
import org.example.graph.subgraph.diagnosis.*;
import org.example.graph.subgraph.knowledge.*;
import org.example.hook.HookEngine;
import org.example.memory.MemoryService;
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
            StepHandlerRegistry handlerRegistry
    ) throws GraphStateException {

        KeyStrategyFactory stateFactory = new PowerOpsStateFactory();
        ChatClient chatClient = chatClientBuilder.build();

        ChatAgentNode chatAgentNode = new ChatAgentNode(dashScopeApiKey, tools);

        StateGraph graph = new StateGraph("power_ops_workflow", stateFactory)
                .addNode("pre_check", node_async(new PreCheckNode()))
                .addNode("context_load", node_async(new ContextLoadNode(memoryService, skillSelector)))
                .addNode("router", node_async(new RouterNode(routerAgent)))
                .addNode("query_rewrite", node_async(new QueryRewriteNode(rbacService)))
                .addNode("rag_retrieve", node_async(new RagRetrieveNode(hybridSearchService)))
                .addNode("tool_execute", node_async(new ToolExecuteNode(toolAgent, dashScopeApiKey)))
                .addNode("rerank", node_async(new RerankNode(rerankService)))
                .addNode("answer_generate", node_async(new AnswerGenerateNode(chatClient, knowledgeGraphService)))
                .addNode("citation_check", node_async(new CitationCheckNode()))
                .addNode("entity_extract", node_async(new EntityExtractNode()))
                .addNode("evidence_collect", node_async(new EvidenceCollectNode(analysisAgent, toolAgent, dashScopeApiKey)))
                .addNode("diagnosis", node_async(new DiagnosisNode(diagnosisAgent, dashScopeApiKey)))
                .addNode("risk_assessment", node_async(new RiskAssessmentNode(riskReviewAgent, dashScopeApiKey)))
                .addNode("replanner", node_async(new ReplannerNode(chatClient)))
                .addNode("action_recommend", node_async(new ActionRecommendNode()))
                .addNode("chat", node_async(chatAgentNode))
                .addNode("safety_review", node_async(new SafetyReviewNode(hookEngine)))
                .addNode("final_response", node_async(new FinalResponseNode()))
                .addNode("memory_save", node_async(new MemorySaveNode(memoryService)))
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
                .addEdge("rerank", "answer_generate")
                .addEdge("answer_generate", "citation_check")
                .addEdge("citation_check", "safety_review")
                .addEdge("entity_extract", "evidence_collect")
                .addEdge("evidence_collect", "diagnosis")
                .addEdge("diagnosis", "risk_assessment")
                .addEdge("risk_assessment", "replanner")
                .addConditionalEdges("replanner", edge_async(new ReplannerDispatcher()),
                        Map.of(
                                "evidence_collect", "evidence_collect",
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
            logger.warn("图可视化生成失败: {}", e.getMessage());
        }

        return graph;
    }

    @Bean
    public CompiledGraph compiledPowerOpsGraph(StateGraph powerOpsGraph) throws GraphStateException {
        return powerOpsGraph.compile();
    }
}
