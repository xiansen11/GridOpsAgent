package org.example.rag;

import org.example.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {

    private static final Logger logger = LoggerFactory.getLogger(HybridSearchService.class);

    @Autowired
    private VectorSearchService vectorSearchService;

    private static final double VECTOR_WEIGHT = 0.7;
    private static final double KEYWORD_WEIGHT = 0.3;

    public List<HybridSearchResult> hybridSearch(String query, int topK) {
        logger.info("执行混合检索: query={}, topK={}", query, topK);

        List<HybridSearchResult> vectorResults = vectorSearch(query, topK * 2);
        List<HybridSearchResult> keywordResults = keywordSearch(query, topK * 2);

        Map<String, HybridSearchResult> merged = new LinkedHashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            HybridSearchResult r = vectorResults.get(i);
            double rrfScore = VECTOR_WEIGHT / (i + 1 + 60);
            r.setScore(rrfScore);
            merged.put(r.getId(), r);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            HybridSearchResult r = keywordResults.get(i);
            double rrfScore = KEYWORD_WEIGHT / (i + 1 + 60);
            if (merged.containsKey(r.getId())) {
                merged.get(r.getId()).setScore(merged.get(r.getId()).getScore() + rrfScore);
            } else {
                r.setScore(rrfScore);
                merged.put(r.getId(), r);
            }
        }

        List<HybridSearchResult> results = merged.values().stream()
                .sorted(Comparator.comparingDouble(HybridSearchResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        logger.info("混合检索完成: 向量结果={}, 关键词结果={}, 合并后={}",
                vectorResults.size(), keywordResults.size(), results.size());

        return results;
    }

    private List<HybridSearchResult> vectorSearch(String query, int topK) {
        try {
            List<VectorSearchService.SearchResult> results = vectorSearchService.searchSimilarDocuments(query, topK);
            return results.stream()
                    .map(r -> new HybridSearchResult(r.getId(), r.getContent(), r.getScore(), "vector"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("向量检索失败，降级为空结果: {}", e.getMessage());
            return List.of();
        }
    }

    private List<HybridSearchResult> keywordSearch(String query, int topK) {
        List<HybridSearchResult> results = new ArrayList<>();

        String[] keywords = query.toLowerCase().split("[\\s,，。、]+");
        List<String> knowledgeEntries = getKeywordIndex();

        for (String entry : knowledgeEntries) {
            double score = 0;
            String lowerEntry = entry.toLowerCase();
            for (String keyword : keywords) {
                if (keyword.length() > 1 && lowerEntry.contains(keyword)) {
                    score += 1.0;
                }
            }
            if (score > 0) {
                results.add(new HybridSearchResult(
                        "kw-" + UUID.randomUUID().toString().substring(0, 8),
                        entry, score, "keyword"));
            }
        }

        results.sort(Comparator.comparingDouble(HybridSearchResult::getScore).reversed());
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    private List<String> getKeywordIndex() {
        return List.of(
                "变压器油温异常升高可能原因：冷却器故障、过负荷运行、内部故障、环境温度过高",
                "开关柜局放超标分析：绝缘老化、表面污秽、接触不良、设计缺陷",
                "配电线路跳闸抢修流程：故障定位→隔离故障段→转移负荷→抢修→恢复送电",
                "高压室巡检安规要求：必须两人同行、穿戴绝缘防护用品、禁止触碰带电设备",
                "倒闸操作安全规程：操作票制度、唱票复诵制度、监护制度、防误操作闭锁",
                "主变油温监测标准：上层油温不超过95℃、温升不超过55K、冷却器全停时限10分钟",
                "GIS设备SF6气体压力监测：额定压力0.6MPa、报警压力0.52MPa、闭锁压力0.50MPa",
                "电力安全工作规程第5.3.2条：在电气设备上工作，必须完成停电、验电、装设接地线、悬挂标示牌",
                "设备缺陷分类：紧急缺陷（24小时内处理）、重大缺陷（7天内处理）、一般缺陷（30天内处理）",
                "红外测温检测标准：温差超过10K需关注、超过15K需跟踪、超过25K需紧急处理"
        );
    }

    public static class HybridSearchResult {
        private final String id;
        private final String content;
        private double score;
        private final String source;

        public HybridSearchResult(String id, String content, double score, String source) {
            this.id = id;
            this.content = content;
            this.score = score;
            this.source = source;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getSource() { return source; }
    }
}
