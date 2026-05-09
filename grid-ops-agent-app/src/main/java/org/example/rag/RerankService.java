package org.example.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RerankService {

    private static final Logger logger = LoggerFactory.getLogger(RerankService.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    public List<RerankResult> rerank(String query, List<HybridSearchService.HybridSearchResult> documents, int topK) {
        logger.info("执行重排序: query={}, documents={}, topK={}", query, documents.size(), topK);

        List<RerankResult> results = new ArrayList<>();
        for (HybridSearchService.HybridSearchResult doc : documents) {
            double relevanceScore = calculateRelevance(query, doc.getContent());
            results.add(new RerankResult(doc.getId(), doc.getContent(), relevanceScore, doc.getSource()));
        }

        results.sort(Comparator.comparingDouble(RerankResult::getRelevanceScore).reversed());

        List<RerankResult> topResults = results.stream()
                .limit(topK)
                .collect(Collectors.toList());

        logger.info("重排序完成，返回 {} 个结果", topResults.size());
        return topResults;
    }

    private double calculateRelevance(String query, String document) {
        double score = 0.0;
        String lowerQuery = query.toLowerCase();
        String lowerDoc = document.toLowerCase();

        String[] queryTerms = lowerQuery.split("[\\s,，。、]+");
        int matchedTerms = 0;
        for (String term : queryTerms) {
            if (term.length() > 1 && lowerDoc.contains(term)) {
                matchedTerms++;
            }
        }
        score += (double) matchedTerms / queryTerms.length * 0.4;

        if (lowerDoc.contains(lowerQuery)) {
            score += 0.3;
        }

        if (lowerDoc.contains("安规") || lowerDoc.contains("安全") || lowerDoc.contains("规程")) {
            if (lowerQuery.contains("安规") || lowerQuery.contains("安全") || lowerQuery.contains("规程")) {
                score += 0.2;
            }
        }

        if (lowerDoc.contains("变压器") && lowerQuery.contains("变压器")) {
            score += 0.1;
        } else if (lowerDoc.contains("开关柜") && lowerQuery.contains("开关柜")) {
            score += 0.1;
        } else if (lowerDoc.contains("线路") && lowerQuery.contains("线路")) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    public static class RerankResult {
        private final String id;
        private final String content;
        private final double relevanceScore;
        private final String source;

        public RerankResult(String id, String content, double relevanceScore, String source) {
            this.id = id;
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.source = source;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public double getRelevanceScore() { return relevanceScore; }
        public String getSource() { return source; }
    }
}
