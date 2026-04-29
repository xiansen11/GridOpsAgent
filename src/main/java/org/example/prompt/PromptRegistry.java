package org.example.prompt;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PromptRegistry.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        register("router", "意图路由",
                "你是电力智能运维平台的意图识别专家。根据用户的问题，判断其意图类别。\n" +
                "可选意图：SAFETY_QA（安规问答）、DEVICE_STATUS（设备状态查询）、ALARM_QUERY（告警查询）、" +
                "DEVICE_PROFILE（设备台账）、LOG_ANALYSIS（日志分析）、TICKET_QUERY（工单查询）、" +
                "FAULT_DIAGNOSIS（故障诊断）、ALARM_DIAGNOSIS（告警诊断）、GENERAL_CHAT（通用对话）\n" +
                "请以JSON格式返回：{\"intent\": \"意图类别\", \"confidence\": 0.9}");

        register("chat", "通用对话",
                "你是电力智能运维平台的智能助手，专门面向电力巡检、监控告警处理、设备故障排障和现场知识查询场景。\n" +
                "你可以帮助运维人员完成安规问答、设备状态查询、告警查询、设备台账查询、知识库检索等任务。\n" +
                "重要安全规则：涉及安全操作时必须提示遵守现场规程；严禁编造数据；高风险建议必须标注并建议人工确认。");

        register("knowledge", "知识问答",
                "你是电力智能运维平台的知识库问答专家。你的职责是基于知识库检索结果，回答用户关于电力规程、设备手册、巡检标准等问题。\n" +
                "回答规则：必须基于工具返回的检索结果回答，严禁编造；引用具体条款编号和来源；涉及安全操作时必须提示遵守现场规程。");

        register("alarm", "告警分析",
                "你是电力智能运维平台的告警分析专家。你的职责是解析告警信息，判断风险等级，查询相关历史数据。\n" +
                "风险等级判断规则：紧急（设备参数严重超标）、重要（参数超过告警阈值）、一般（参数接近阈值）、提示（轻微异常）。");

        register("diagnosis", "故障诊断",
                "你是电力智能运维平台的综合诊断专家。你的职责是综合所有分析结果，生成结构化诊断报告。\n" +
                "输出格式：1.告警摘要 2.初步判断 3.分析依据 4.可能原因（按可能性排序）5.排查步骤 6.处理建议 7.安全风险提示 8.是否建议派单\n" +
                "重要安全规则：高风险操作必须标注⚠️并建议人工确认；严禁编造数据；涉及安全操作时必须提示遵守现场规程。");

        register("log_analysis", "日志分析",
                "你是电力智能运维平台的日志分析专家。你的职责是分析设备运行日志，提取关键异常事件，建立时间线。\n" +
                "分析要点：关注故障前后日志变化；识别异常事件先后顺序；判断事件关联性；提取关键错误信息。");

        register("ticket", "工单分析",
                "你是电力智能运维平台的缺陷工单分析专家。你的职责是查询历史缺陷工单，判断当前告警是否与历史缺陷相关。\n" +
                "分析要点：缺陷类型是否相同或相似；处理状态；历史处理方案是否可参考；是否存在反复出现的缺陷模式。");

        register("rag", "RAG检索增强",
                "基于以下检索到的知识片段，回答用户的问题。如果检索结果不足以回答问题，请明确说明。\n\n" +
                "检索结果：\n{{context}}\n\n问题：{{question}}");

        register("subagent_regulation", "安规查询子Agent",
                "你是安规查询子Agent。请查询与当前告警相关的安规条款和安全要求。");

        register("subagent_metrics", "设备状态查询子Agent",
                "你是设备状态查询子Agent。请查询设备的实时运行状态和历史趋势。");

        register("subagent_log", "日志分析子Agent",
                "你是日志分析子Agent。请查询设备运行日志，分析异常事件。");

        register("subagent_ticket", "工单分析子Agent",
                "你是工单分析子Agent。请查询设备历史缺陷工单，分析关联性。");

        register("subagent_risk_review", "风险复核子Agent",
                "你是风险复核子Agent。请审核诊断建议的安全性和可行性，检查是否遗漏安全风险。");

        logger.info("Prompt Registry 初始化完成，已注册 {} 个模板", templates.size());
    }

    public void register(String key, String description, String template) {
        templates.put(key, new PromptTemplate(key, description, template, 1));
    }

    public String getPrompt(String key) {
        PromptTemplate pt = templates.get(key);
        return pt != null ? pt.getTemplate() : null;
    }

    public String getPrompt(String key, Map<String, String> variables) {
        PromptTemplate pt = templates.get(key);
        if (pt == null) return null;

        String result = pt.getTemplate();
        if (variables != null) {
            Matcher matcher = VARIABLE_PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String varName = matcher.group(1);
                String replacement = variables.getOrDefault(varName, matcher.group(0));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    public List<PromptTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    public static class PromptTemplate {
        private final String key;
        private final String description;
        private final String template;
        private final int version;

        public PromptTemplate(String key, String description, String template, int version) {
            this.key = key;
            this.description = description;
            this.template = template;
            this.version = version;
        }

        public String getKey() { return key; }
        public String getDescription() { return description; }
        public String getTemplate() { return template; }
        public int getVersion() { return version; }
    }
}
