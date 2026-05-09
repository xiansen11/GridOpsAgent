package org.example.graph.subgraph.diagnosis;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.rag.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AlarmRagRetrieveNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(AlarmRagRetrieveNode.class);
    private final HybridSearchService hybridSearchService;

    public AlarmRagRetrieveNode(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("cleaned_input").map(Object::toString).orElse("");
        Object entitiesObj = state.value("entities").orElse(Map.of());

        logger.info("AlarmRagRetrieveNode: 告警相关文档检索");

        StringBuilder searchQuery = new StringBuilder(input);
        if (entitiesObj instanceof Map<?, ?> entities && !entities.isEmpty()) {
            searchQuery.append(" ");
            if (entities.containsKey("deviceType")) {
                searchQuery.append(entities.get("deviceType"));
            }
            if (entities.containsKey("faultType")) {
                searchQuery.append(" ").append(entities.get("faultType"));
            }
        }

        List<HybridSearchService.HybridSearchResult> results = hybridSearchService.hybridSearch(searchQuery.toString(), 5);
        StringBuilder context = new StringBuilder();
        for (HybridSearchService.HybridSearchResult r : results) {
            context.append(r.getContent()).append("\n\n");
        }

        logger.info("AlarmRagRetrieveNode: 检索完成，获取{}条结果", results.size());
        return Map.of(
                "execution_result", context.toString(),
                "rag_results", results
        );
    }
}
