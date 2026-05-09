package org.example.checkpoint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.CheckpointRecord;
import org.example.graph.GraphStateKeys;
import org.example.mapper.CheckpointRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(CheckpointService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CheckpointRecordMapper checkpointRecordMapper;

    public void saveCheckpoint(String taskId, String sessionId, String stepName, Map<String, Object> state) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        try {
            Map<String, Object> normalizedState = normalizeState(state);
            CheckpointRecord record = CheckpointRecord.builder()
                    .taskId(taskId)
                    .stepName(stepName)
                    .agentState(toJson(normalizedState))
                    .planSteps(toJson(normalizedState.get(GraphStateKeys.PLAN_STEPS)))
                    .completedSteps(toJson(normalizedState.get(GraphStateKeys.STEP_RESULTS)))
                    .ragResults(toJson(normalizedState.get(GraphStateKeys.RAG_RESULTS)))
                    .toolResults(toJson(normalizedState.get(GraphStateKeys.TOOL_RESULT)))
                    .subagentResults(toJson(normalizedState.get("subagent_results")))
                    .diagnosisDraft(stringValue(normalizedState.get(GraphStateKeys.DIAGNOSIS_RESULT)))
                    .approvalStatus(stringValue(normalizedState.getOrDefault("approval_status", "PENDING")))
                    .errorMessage(stringValue(normalizedState.get("error_message")))
                    .createdAt(LocalDateTime.now())
                    .build();

            checkpointRecordMapper.insert(record);
            logger.info("Saved checkpoint: taskId={}, step={}, sessionId={}", taskId, stepName, sessionId);
        } catch (Exception e) {
            logger.error("Failed to save checkpoint: taskId={}", taskId, e);
        }
    }

    public void saveCheckpoint(String taskId, String stepName, Map<String, Object> state) {
        saveCheckpoint(taskId, null, stepName, state);
    }

    public Optional<CheckpointRecord> getLatestCheckpoint(String taskId) {
        CheckpointRecord record = checkpointRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckpointRecord>()
                        .eq(CheckpointRecord::getTaskId, taskId)
                        .orderByDesc(CheckpointRecord::getCreatedAt)
                        .last("LIMIT 1"));
        return Optional.ofNullable(record);
    }

    public List<CheckpointRecord> getAllCheckpoints(String taskId) {
        return checkpointRecordMapper.selectList(
                new LambdaQueryWrapper<CheckpointRecord>()
                        .eq(CheckpointRecord::getTaskId, taskId)
                        .orderByAsc(CheckpointRecord::getCreatedAt));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> restoreFromCheckpoint(String taskId) {
        Optional<CheckpointRecord> latest = getLatestCheckpoint(taskId);
        if (latest.isEmpty()) {
            return null;
        }

        CheckpointRecord record = latest.get();
        Map<String, Object> state = new LinkedHashMap<>();
        Object agentState = fromJson(record.getAgentState());
        if (agentState instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                state.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        state.put(GraphStateKeys.TASK_ID, record.getTaskId());
        state.put("step_name", record.getStepName());
        putIfAbsent(state, GraphStateKeys.PLAN_STEPS, fromJson(record.getPlanSteps()));
        putIfAbsent(state, GraphStateKeys.STEP_RESULTS, fromJson(record.getCompletedSteps()));
        putIfAbsent(state, GraphStateKeys.RAG_RESULTS, fromJson(record.getRagResults()));
        putIfAbsent(state, GraphStateKeys.TOOL_RESULT, fromJson(record.getToolResults()));
        putIfAbsent(state, GraphStateKeys.DIAGNOSIS_RESULT, record.getDiagnosisDraft());
        state.put("approval_status", record.getApprovalStatus());

        logger.info("Restored checkpoint: taskId={}, step={}", taskId, record.getStepName());
        return normalizeState(state);
    }

    public void cleanupOldCheckpoints(String taskId, int keepLatest) {
        List<CheckpointRecord> all = getAllCheckpoints(taskId);
        if (all.size() > keepLatest) {
            for (int i = 0; i < all.size() - keepLatest; i++) {
                checkpointRecordMapper.deleteById(all.get(i).getId());
            }
            logger.info("Cleaned old checkpoints: taskId={}, deleted={}", taskId, all.size() - keepLatest);
        }
    }

    private Map<String, Object> normalizeState(Map<String, Object> state) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (state != null) {
            normalized.putAll(state);
        }
        alias(normalized, "taskId", GraphStateKeys.TASK_ID);
        alias(normalized, "sessionId", GraphStateKeys.SESSION_ID);
        alias(normalized, "planSteps", GraphStateKeys.PLAN_STEPS);
        alias(normalized, "completedSteps", GraphStateKeys.STEP_RESULTS);
        alias(normalized, "ragResults", GraphStateKeys.RAG_RESULTS);
        alias(normalized, "toolResults", GraphStateKeys.TOOL_RESULT);
        alias(normalized, "diagnosisDraft", GraphStateKeys.DIAGNOSIS_RESULT);
        return normalized;
    }

    private void alias(Map<String, Object> state, String oldKey, String newKey) {
        if (!state.containsKey(newKey) && state.containsKey(oldKey)) {
            state.put(newKey, state.get(oldKey));
        }
    }

    private void putIfAbsent(Map<String, Object> state, String key, Object value) {
        if (!state.containsKey(key) && value != null) {
            state.put(key, value);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private Object fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
