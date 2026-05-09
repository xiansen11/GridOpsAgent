package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.rag.HybridSearchService;
import org.example.rag.RerankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class RerankNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RerankNode.class);
    private final RerankService rerankService;

    public RerankNode(RerankService rerankService) {
        this.rerankService = rerankService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("RerankNode: 重排序");

        Object ragResultsObj = state.value("rag_results").orElse(null);
        List<HybridSearchService.HybridSearchResult> ragResults;

        if (ragResultsObj instanceof List<?> list) {
            ragResults = (List<HybridSearchService.HybridSearchResult>) list;
        } else {
            String executionResult = state.value("execution_result").map(Object::toString).orElse("");
            if (executionResult.isEmpty()) {
                return Map.of("execution_result", "");
            }
            return Map.of("execution_result", executionResult);
        }

        if (ragResults.isEmpty()) {
            String executionResult = state.value("execution_result").map(Object::toString).orElse("");
            return Map.of("execution_result", executionResult);
        }

        List<RerankService.RerankResult> rerankedResults = rerankService.rerank(input, ragResults, 3);
        StringBuilder rerankedContext = new StringBuilder();
        for (RerankService.RerankResult r : rerankedResults) {
            rerankedContext.append(r.getContent()).append("\n\n");
        }

        logger.info("RerankNode: 重排序完成，从{}条结果中选出{}条", ragResults.size(), rerankedResults.size());
        return Map.of("execution_result", rerankedContext.toString());
    }
}
