package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.rag.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RagRetrieveNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RagRetrieveNode.class);
    private final HybridSearchService hybridSearchService;

    public RagRetrieveNode(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("RagRetrieveNode: Hybrid RAG检索, input={}", input);

        var results = hybridSearchService.hybridSearch(input, 5);
        StringBuilder context = new StringBuilder();
        for (var r : results) {
            context.append(r.getContent()).append("\n\n");
        }

        return Map.of("execution_result", context.toString());
    }
}
