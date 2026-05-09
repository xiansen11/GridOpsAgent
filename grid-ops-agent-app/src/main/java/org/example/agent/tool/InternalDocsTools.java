package org.example.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InternalDocsTools {

    private static final Logger logger = LoggerFactory.getLogger(InternalDocsTools.class);

    public static final String TOOL_QUERY_INTERNAL_DOCS = "queryInternalDocs";

    private final VectorSearchService vectorSearchService;

    @Value("${rag.top-k:3}")
    private int topK = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public InternalDocsTools(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @Tool(description = "搜索电力运维知识库中的相关文档和规程。" +
            "可以检索电力安规、设备手册、巡检标准、故障处理规程、历史故障案例、专家经验文档等。" +
            "当需要查询专业知识、操作规程、故障处理步骤等信息时使用此工具。")
    public String queryInternalDocs(
            @ToolParam(description = "搜索查询，描述您要查找的电力运维信息，如 主变油温异常处理步骤,高压室作业安全措施")
            String query) {

        try {
            List<VectorSearchService.SearchResult> searchResults =
                    vectorSearchService.searchSimilarDocuments(query, topK);

            if (searchResults.isEmpty()) {
                return "{\"status\": \"no_results\", \"message\": \"知识库中未找到相关文档，请尝试使用其他工具查询。\"}";
            }

            String resultJson = objectMapper.writeValueAsString(searchResults);
            return resultJson;

        } catch (Exception e) {
            logger.error("queryInternalDocs 执行失败", e);
            return String.format("{\"status\": \"error\", \"message\": \"知识库查询失败: %s\"}",
                    e.getMessage());
        }
    }
}
