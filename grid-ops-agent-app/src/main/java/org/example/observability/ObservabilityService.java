package org.example.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.entity.AgentExecutionLog;
import org.example.entity.ToolCallLog;
import org.example.mapper.AgentExecutionLogMapper;
import org.example.mapper.ToolCallLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ObservabilityService {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityService.class);

    @Autowired
    private AgentExecutionLogMapper executionLogMapper;

    @Autowired
    private ToolCallLogMapper toolCallLogMapper;

    private final ThreadLocal<Deque<Span>> spanStack = ThreadLocal.withInitial(ArrayDeque::new);

    public String generateTraceId() {
        return "TRACE-" + UUID.randomUUID().toString().substring(0, 12);
    }

    public Span startSpan(String traceId, String taskId, String sessionId,
                           String agentName, String stepName, String inputSummary) {
        Span span = new Span();
        span.setTraceId(traceId);
        span.setSpanId("SPAN-" + UUID.randomUUID().toString().substring(0, 8));
        span.setTaskId(taskId);
        span.setSessionId(sessionId);
        span.setAgentName(agentName);
        span.setStepName(stepName);
        span.setInputSummary(inputSummary);
        span.setStartTime(System.currentTimeMillis());

        Deque<Span> stack = spanStack.get();
        if (!stack.isEmpty()) {
            span.setParentSpanId(stack.peek().getSpanId());
        }
        stack.push(span);

        return span;
    }

    public Span endSpan(String status, String outputSummary, Integer tokenCount) {
        Deque<Span> stack = spanStack.get();
        if (stack.isEmpty()) {
            logger.warn("No active span to end");
            return null;
        }

        Span span = stack.pop();
        span.setEndTime(System.currentTimeMillis());
        span.setDurationMs(span.getEndTime() - span.getStartTime());
        span.setStatus(status);
        span.setOutputSummary(outputSummary);
        span.setTokenCount(tokenCount);

        logAgentExecution(span.getTraceId(), span.getTaskId(), span.getSessionId(),
                span.getAgentName(), span.getStepName(),
                span.getInputSummary(), span.getOutputSummary(),
                span.getStatus(), span.getDurationMs(), span.getTokenCount());

        return span;
    }

    public void logAgentExecution(String traceId, String taskId, String sessionId,
                                   String agentName, String stepName,
                                   String inputSummary, String outputSummary,
                                   String status, long durationMs, Integer tokenCount) {
        try {
            AgentExecutionLog logEntry = AgentExecutionLog.builder()
                    .traceId(traceId)
                    .taskId(taskId)
                    .sessionId(sessionId)
                    .nodeName(stepName)
                    .agentName(agentName)
                    .stepName(stepName)
                    .inputSummary(truncate(inputSummary, 500))
                    .outputSummary(truncate(outputSummary, 500))
                    .status(status)
                    .durationMs(durationMs)
                    .tokenCount(tokenCount)
                    .createdAt(LocalDateTime.now())
                    .build();

            executionLogMapper.insert(logEntry);
        } catch (Exception e) {
            logger.error("记录Agent执行日志失败", e);
        }
    }

    public void logToolCall(String traceId, String taskId, String sessionId,
                             String toolName, String requestParam, String responseData,
                             String status, long durationMs) {
        try {
            ToolCallLog logEntry = ToolCallLog.builder()
                    .traceId(traceId)
                    .taskId(taskId)
                    .sessionId(sessionId)
                    .toolName(toolName)
                    .toolSource(toolName != null && toolName.startsWith("getDevice") || "getAlarmHistory".equals(toolName) || "getDefectTickets".equals(toolName)
                            ? "MCP" : "LOCAL")
                    .requestParam(truncate(requestParam, 1000))
                    .responseData(truncate(responseData, 1000))
                    .status(status)
                    .durationMs(durationMs)
                    .createdAt(LocalDateTime.now())
                    .build();

            toolCallLogMapper.insert(logEntry);
        } catch (Exception e) {
            logger.error("记录工具调用日志失败", e);
        }
    }

    public List<AgentExecutionLog> getTrace(String traceId) {
        return executionLogMapper.selectList(
                new LambdaQueryWrapper<AgentExecutionLog>()
                        .eq(AgentExecutionLog::getTraceId, traceId)
                        .orderByAsc(AgentExecutionLog::getCreatedAt));
    }

    public List<ToolCallLog> getToolCallLogs(String traceId) {
        return toolCallLogMapper.selectList(
                new LambdaQueryWrapper<ToolCallLog>()
                        .eq(ToolCallLog::getTraceId, traceId)
                        .orderByAsc(ToolCallLog::getCreatedAt));
    }

    public Map<String, Object> getTraceSummary(String traceId) {
        List<AgentExecutionLog> agentLogs = getTrace(traceId);
        List<ToolCallLog> toolLogs = getToolCallLogs(traceId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("traceId", traceId);
        summary.put("agentSteps", agentLogs.size());
        summary.put("toolCalls", toolLogs.size());

        long totalDuration = agentLogs.stream()
                .mapToLong(l -> l.getDurationMs() != null ? l.getDurationMs() : 0)
                .sum();
        summary.put("totalDurationMs", totalDuration);

        int totalTokens = agentLogs.stream()
                .mapToInt(l -> l.getTokenCount() != null ? l.getTokenCount() : 0)
                .sum();
        summary.put("totalTokens", totalTokens);

        List<String> agents = agentLogs.stream()
                .map(AgentExecutionLog::getAgentName)
                .distinct()
                .toList();
        summary.put("agents", agents);

        List<String> tools = toolLogs.stream()
                .map(ToolCallLog::getToolName)
                .distinct()
                .toList();
        summary.put("tools", tools);

        long failedSteps = agentLogs.stream().filter(log -> "FAILED".equalsIgnoreCase(log.getStatus())).count();
        long failedTools = toolLogs.stream().filter(log -> "FAILED".equalsIgnoreCase(log.getStatus())).count();
        long replanCount = agentLogs.stream().filter(log -> "replanner".equalsIgnoreCase(log.getStepName())).count();
        summary.put("failedSteps", failedSteps);
        summary.put("failedToolCalls", failedTools);
        summary.put("replanCount", replanCount);

        return summary;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    public static class Span {
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String taskId;
        private String sessionId;
        private String agentName;
        private String stepName;
        private String inputSummary;
        private String outputSummary;
        private String status;
        private long startTime;
        private long endTime;
        private long durationMs;
        private Integer tokenCount;

        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        public String getSpanId() { return spanId; }
        public void setSpanId(String spanId) { this.spanId = spanId; }
        public String getParentSpanId() { return parentSpanId; }
        public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        public String getInputSummary() { return inputSummary; }
        public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }
        public String getOutputSummary() { return outputSummary; }
        public void setOutputSummary(String outputSummary) { this.outputSummary = outputSummary; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public Integer getTokenCount() { return tokenCount; }
        public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    }
}
