package org.example.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.Getter;
import lombok.Setter;
import org.example.constant.MilvusConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Autowired
    private VectorEmbeddingService embeddingService;

    @Autowired
    private InMemoryVectorStore inMemoryVectorStore;

    @Value("${milvus.enabled:false}")
    private boolean milvusEnabled;

    public List<SearchResult> searchSimilarDocuments(String query, int topK) {
        try {
            logger.info("开始搜索相似文档, 查询: {}, topK: {}", query, topK);

            List<Float> queryVector = embeddingService.generateQueryVector(query);
            logger.debug("查询向量生成成功, 维度: {}", queryVector.size());

            if (milvusEnabled && milvusClient != null) {
                return searchFromMilvus(queryVector, topK);
            } else {
                return searchFromMemory(queryVector, topK);
            }

        } catch (Exception e) {
            logger.error("搜索相似文档失败", e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    private List<SearchResult> searchFromMemory(List<Float> queryVector, int topK) {
        logger.info("使用内存向量存储进行搜索");
        List<InMemoryVectorStore.SearchResult> memoryResults = inMemoryVectorStore.search(queryVector, topK);
        
        List<SearchResult> results = new ArrayList<>();
        for (InMemoryVectorStore.SearchResult mr : memoryResults) {
            SearchResult result = new SearchResult();
            result.setId(mr.getId());
            result.setContent(mr.getContent());
            result.setScore(mr.getScore());
            if (mr.getMetadata() != null) {
                result.setMetadata(mr.getMetadata().toString());
            }
            results.add(result);
        }
        
        logger.info("内存搜索完成, 找到 {} 个相似文档", results.size());
        return results;
    }

    private List<SearchResult> searchFromMilvus(List<Float> queryVector, int topK) {
        logger.info("使用 Milvus 进行搜索");
        
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                .withVectorFieldName("vector")
                .withVectors(Collections.singletonList(queryVector))
                .withTopK(topK)
                .withMetricType(io.milvus.param.MetricType.L2)
                .withOutFields(List.of("id", "content", "metadata"))
                .withParams("{\"nprobe\":10}")
                .build();

        R<SearchResults> searchResponse = milvusClient.search(searchParam);

        if (searchResponse.getStatus() != 0) {
            throw new RuntimeException("向量搜索失败: " + searchResponse.getMessage());
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            SearchResult result = new SearchResult();
            result.setId((String) wrapper.getIDScore(0).get(i).get("id"));
            result.setContent((String) wrapper.getFieldData("content", 0).get(i));
            result.setScore(wrapper.getIDScore(0).get(i).getScore());
            
            Object metadataObj = wrapper.getFieldData("metadata", 0).get(i);
            if (metadataObj != null) {
                result.setMetadata(metadataObj.toString());
            }
            
            results.add(result);
        }

        logger.info("Milvus 搜索完成, 找到 {} 个相似文档", results.size());
        return results;
    }

    @Setter
    @Getter
    public static class SearchResult {
        private String id;
        private String content;
        private float score;
        private String metadata;
    }
}
