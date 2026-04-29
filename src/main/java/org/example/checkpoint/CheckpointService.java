package org.example.checkpoint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.CheckpointRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(CheckpointService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private org.example.mapper.CheckpointRecordMapper checkpointRecordMapper;

    public void saveCheckpoint(String taskId, String sessionId, String stepName, Map<String, Object> state) {
        try {
            CheckpointRecord record = CheckpointRecord.builder()
                    .taskId(taskId)
                    .stepName(stepName)
                    .agentState(toJson(state))
                    .planSteps(toJson(state.get("planSteps")))
                    .completedSteps(toJson(state.get("completedSteps")))
                    .ragResults(toJson(state.get("ragResults")))
                    .toolResults(toJson(state.get("toolResults")))
                    .subagentResults(toJson(state.get("subagentResults")))
                    .diagnosisDraft(state.get("diagnosisDraft") != null ? state.get("diagnosisDraft").toString() : null)
                    .approvalStatus((String) state.getOrDefault("approvalStatus", "PENDING"))
                    .createdAt(LocalDateTime.now())
                    .build();

            checkpointRecordMapper.insert(record);
            logger.info("保存检查点: taskId={}, step={}, sessionId={}", taskId, stepName, sessionId);
        } catch (Exception e) {
            logger.error("保存检查点失败: taskId={}", taskId, e);
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

    public Map<String, Object> restoreFromCheckpoint(String taskId) {
        Optional<CheckpointRecord> latest = getLatestCheckpoint(taskId);
        if (latest.isEmpty()) {
            return null;
        }

        CheckpointRecord record = latest.get();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("taskId", record.getTaskId());
        state.put("stepName", record.getStepName());
        state.put("agentState", fromJson(record.getAgentState()));
        state.put("planSteps", fromJson(record.getPlanSteps()));
        state.put("completedSteps", fromJson(record.getCompletedSteps()));
        state.put("ragResults", fromJson(record.getRagResults()));
        state.put("toolResults", fromJson(record.getToolResults()));
        state.put("subagentResults", fromJson(record.getSubagentResults()));
        state.put("diagnosisDraft", record.getDiagnosisDraft());
        state.put("approvalStatus", record.getApprovalStatus());

        Map<String, Object> agentState = (Map<String, Object>) fromJson(record.getAgentState());
        if (agentState != null) {
            state.putAll(agentState);
        }

        logger.info("从检查点恢复: taskId={}, step={}", taskId, record.getStepName());
        return state;
    }

    public void cleanupOldCheckpoints(String taskId, int keepLatest) {
        List<CheckpointRecord> all = getAllCheckpoints(taskId);
        if (all.size() > keepLatest) {
            for (int i = 0; i < all.size() - keepLatest; i++) {
                checkpointRecordMapper.deleteById(all.get(i).getId());
            }
            logger.info("清理旧检查点: taskId={}, 删除{}条", taskId, all.size() - keepLatest);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private Object fromJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
