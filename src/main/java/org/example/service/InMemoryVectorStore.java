package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class InMemoryVectorStore {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final Map<String, VectorDocument> documents = new ConcurrentHashMap<>();

    public void insert(String id, String content, List<Float> vector, Map<String, Object> metadata) {
        VectorDocument doc = new VectorDocument(id, content, new ArrayList<>(vector), metadata);
        documents.put(id, doc);
        logger.debug("Inserted document: id={}, content length={}", id, content.length());
    }

    public void deleteBySource(String source) {
        documents.entrySet().removeIf(entry -> {
            Map<String, Object> metadata = entry.getValue().getMetadata();
            if (metadata != null && source.equals(metadata.get("_source"))) {
                logger.debug("Deleted document: id={}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    public void deleteByDocumentId(String documentId) {
        documents.entrySet().removeIf(entry -> {
            Map<String, Object> metadata = entry.getValue().getMetadata();
            if (metadata != null && documentId.equals(metadata.get("documentId"))) {
                logger.debug("Deleted document by documentId: id={}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    public List<SearchResult> search(List<Float> queryVector, int topK) {
        if (documents.isEmpty()) {
            return new ArrayList<>();
        }

        return documents.values().parallelStream()
                .map(doc -> {
                    float score = cosineSimilarity(queryVector, doc.getVector());
                    return new SearchResult(doc.getId(), doc.getContent(), score, doc.getMetadata());
                })
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private float cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public int size() {
        return documents.size();
    }

    public void clear() {
        documents.clear();
    }

    public static class VectorDocument {
        private final String id;
        private final String content;
        private final List<Float> vector;
        private final Map<String, Object> metadata;

        public VectorDocument(String id, String content, List<Float> vector, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.vector = vector;
            this.metadata = metadata;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public List<Float> getVector() { return vector; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    public static class SearchResult {
        private final String id;
        private final String content;
        private final float score;
        private final Map<String, Object> metadata;

        public SearchResult(String id, String content, float score, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.score = score;
            this.metadata = metadata;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public float getScore() { return score; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
