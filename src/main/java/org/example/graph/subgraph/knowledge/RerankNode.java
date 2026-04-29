package org.example.graph.subgraph.knowledge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
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
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String context = state.value("execution_result").map(Object::toString).orElse("");
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        logger.info("RerankNode: 重排序");

        String reranked = rerankService.rerank(input, List.of(), 3).stream()
                .map(r -> r.getContent()).reduce("", (a, b) -> a + "\n" + b);
        return Map.of("execution_result", reranked);
    }
}
