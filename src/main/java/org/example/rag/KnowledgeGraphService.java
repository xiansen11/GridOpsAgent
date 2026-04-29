package org.example.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);

    private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();

    public KnowledgeGraphService() {
        initGraph();
    }

    private void initGraph() {
        addNode("transformer", "变压器", "设备类型");
        addNode("switchgear", "开关柜", "设备类型");
        addNode("line", "配电线路", "设备类型");
        addNode("gis", "GIS设备", "设备类型");

        addNode("oil_temp_high", "油温高告警", "告警类型");
        addNode("partial_discharge", "局放超标告警", "告警类型");
        addNode("trip_alarm", "跳闸告警", "告警类型");
        addNode("sf6_low", "SF6压力低告警", "告警类型");

        addNode("cooler_fault", "冷却器故障", "故障原因");
        addNode("overload", "过负荷运行", "故障原因");
        addNode("internal_fault", "内部故障", "故障原因");
        addNode("insulation_aging", "绝缘老化", "故障原因");
        addNode("surface_contamination", "表面污秽", "故障原因");
        addNode("contact_failure", "接触不良", "故障原因");

        addNode("check_oil_temp", "检查油温及冷却器运行状态", "排查步骤");
        addNode("check_load", "检查负荷情况", "排查步骤");
        addNode("check_pd_data", "检查局放检测数据", "排查步骤");
        addNode("check_sf6_pressure", "检查SF6气体压力", "排查步骤");
        addNode("check_insulation", "检查绝缘状态", "排查步骤");

        addNode("safety_regulation_532", "《安规》第5.3.2条", "安规条款");
        addNode("safety_regulation_411", "《安规》第4.1.1条", "安规条款");

        addEdge("transformer", "oil_temp_high", "可能产生");
        addEdge("oil_temp_high", "cooler_fault", "可能原因");
        addEdge("oil_temp_high", "overload", "可能原因");
        addEdge("oil_temp_high", "internal_fault", "可能原因");
        addEdge("oil_temp_high", "check_oil_temp", "排查步骤");
        addEdge("oil_temp_high", "check_load", "排查步骤");
        addEdge("oil_temp_high", "safety_regulation_532", "相关安规");

        addEdge("switchgear", "partial_discharge", "可能产生");
        addEdge("partial_discharge", "insulation_aging", "可能原因");
        addEdge("partial_discharge", "surface_contamination", "可能原因");
        addEdge("partial_discharge", "contact_failure", "可能原因");
        addEdge("partial_discharge", "check_pd_data", "排查步骤");
        addEdge("partial_discharge", "check_insulation", "排查步骤");
        addEdge("partial_discharge", "safety_regulation_411", "相关安规");

        addEdge("line", "trip_alarm", "可能产生");
        addEdge("gis", "sf6_low", "可能产生");
        addEdge("sf6_low", "check_sf6_pressure", "排查步骤");

        logger.info("知识图谱初始化完成: {} 个节点, {} 条边", nodes.size(), edges.size());
    }

    private void addNode(String id, String name, String type) {
        nodes.put(id, new GraphNode(id, name, type));
    }

    private void addEdge(String sourceId, String targetId, String relation) {
        edges.add(new GraphEdge(sourceId, targetId, relation));
    }

    public GraphExpansionResult expandFromEntity(String entityId, int depth) {
        GraphExpansionResult result = new GraphExpansionResult();
        result.setRootEntity(nodes.get(entityId));

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(entityId);
        visited.add(entityId);

        while (!queue.isEmpty() && depth > 0) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();
                List<GraphEdge> relatedEdges = edges.stream()
                        .filter(e -> e.getSourceId().equals(current) || e.getTargetId().equals(current))
                        .collect(Collectors.toList());

                for (GraphEdge edge : relatedEdges) {
                    String nextId = edge.getSourceId().equals(current) ? edge.getTargetId() : edge.getSourceId();
                    if (!visited.contains(nextId)) {
                        visited.add(nextId);
                        queue.add(nextId);
                        result.getRelatedNodes().add(nodes.get(nextId));
                        result.getRelatedEdges().add(edge);
                    }
                }
            }
            depth--;
        }

        return result;
    }

    public List<GraphNode> findRelatedCauses(String alarmType) {
        return edges.stream()
                .filter(e -> e.getRelation().equals("可能原因") && e.getSourceId().equals(alarmType))
                .map(e -> nodes.get(e.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<GraphNode> findRelatedSteps(String alarmType) {
        return edges.stream()
                .filter(e -> e.getRelation().equals("排查步骤") && e.getSourceId().equals(alarmType))
                .map(e -> nodes.get(e.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<GraphNode> findRelatedRegulations(String alarmType) {
        return edges.stream()
                .filter(e -> e.getRelation().equals("相关安规") && e.getSourceId().equals(alarmType))
                .map(e -> nodes.get(e.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String buildGraphContext(String query) {
        StringBuilder context = new StringBuilder();
        context.append("【知识图谱扩展信息】\n");

        for (Map.Entry<String, GraphNode> entry : nodes.entrySet()) {
            if (query.contains(entry.getValue().getName()) ||
                    query.toLowerCase().contains(entry.getKey())) {
                GraphExpansionResult expansion = expandFromEntity(entry.getKey(), 2);

                List<GraphNode> causes = findRelatedCauses(entry.getKey());
                if (!causes.isEmpty()) {
                    context.append("可能原因: ");
                    context.append(causes.stream().map(GraphNode::getName).collect(Collectors.joining("、")));
                    context.append("\n");
                }

                List<GraphNode> steps = findRelatedSteps(entry.getKey());
                if (!steps.isEmpty()) {
                    context.append("推荐排查步骤: ");
                    context.append(steps.stream().map(GraphNode::getName).collect(Collectors.joining("→")));
                    context.append("\n");
                }

                List<GraphNode> regulations = findRelatedRegulations(entry.getKey());
                if (!regulations.isEmpty()) {
                    context.append("相关安规: ");
                    context.append(regulations.stream().map(GraphNode::getName).collect(Collectors.joining("、")));
                    context.append("\n");
                }

                break;
            }
        }

        return context.toString();
    }

    public static class GraphNode {
        private final String id;
        private final String name;
        private final String type;
        public GraphNode(String id, String name, String type) { this.id = id; this.name = name; this.type = type; }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
    }

    public static class GraphEdge {
        private final String sourceId;
        private final String targetId;
        private final String relation;
        public GraphEdge(String sourceId, String targetId, String relation) { this.sourceId = sourceId; this.targetId = targetId; this.relation = relation; }
        public String getSourceId() { return sourceId; }
        public String getTargetId() { return targetId; }
        public String getRelation() { return relation; }
    }

    public static class GraphExpansionResult {
        private GraphNode rootEntity;
        private final List<GraphNode> relatedNodes = new ArrayList<>();
        private final List<GraphEdge> relatedEdges = new ArrayList<>();
        public GraphNode getRootEntity() { return rootEntity; }
        public void setRootEntity(GraphNode rootEntity) { this.rootEntity = rootEntity; }
        public List<GraphNode> getRelatedNodes() { return relatedNodes; }
        public List<GraphEdge> getRelatedEdges() { return relatedEdges; }
    }
}
