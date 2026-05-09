package org.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.entity.AlarmTask;
import org.example.mapper.AlarmTaskMapper;
import org.example.checkpoint.CheckpointService;
import org.example.entity.CheckpointRecord;
import org.example.graph.GraphStateKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private static final Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Autowired
    private CompiledGraph compiledGraph;

    @Autowired
    private CheckpointService checkpointService;

    @Autowired
    private AlarmTaskMapper alarmTaskMapper;

    private final ExecutorService diagnosisExecutor = Executors.newCachedThreadPool();

    @PostMapping("/receive")
    public Map<String, Object> receiveAlarm(@RequestBody Map<String, Object> alarmEvent) {
        logger.info("接收告警事件: {}", alarmEvent);

        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8);
        String alarmId = (String) alarmEvent.getOrDefault("alarmId", "ALM-" + System.currentTimeMillis());

        AlarmTask alarmTask = AlarmTask.builder()
                .taskId(taskId)
                .alarmId(alarmId)
                .station((String) alarmEvent.get("station"))
                .deviceId((String) alarmEvent.get("deviceId"))
                .deviceName((String) alarmEvent.get("deviceName"))
                .deviceType((String) alarmEvent.get("deviceType"))
                .alarmType((String) alarmEvent.get("alarmType"))
                .alarmLevel((String) alarmEvent.get("alarmLevel"))
                .alarmSource((String) alarmEvent.get("alarmSource"))
                .currentValue((String) alarmEvent.get("currentValue"))
                .threshold((String) alarmEvent.get("threshold"))
                .duration((String) alarmEvent.get("duration"))
                .status("RECEIVED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        alarmTaskMapper.insert(alarmTask);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("taskId", taskId);
        response.put("alarmId", alarmId);
        response.put("status", "RECEIVED");
        response.put("message", "告警已接收，请调用 /api/alarm/diagnose 启动诊断");

        return response;
    }

    @PostMapping(value = "/diagnose", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnoseAlarm(@RequestBody Map<String, String> request) {
        String taskId = request.get("taskId");
        String alarmDescription = request.get("alarmDescription");

        logger.info("启动告警诊断: taskId={}, description={}", taskId, alarmDescription);

        SseEmitter emitter = new SseEmitter(600000L);

        diagnosisExecutor.execute(() -> {
            try {
                AlarmTask alarmTask = null;
                if (taskId != null) {
                    alarmTask = alarmTaskMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlarmTask>()
                                    .eq(AlarmTask::getTaskId, taskId)
                    );
                }

                String diagnosisInput;
                if (alarmTask != null) {
                    diagnosisInput = String.format(
                            "请对以下告警进行诊断分析：\n" +
                                    "告警ID: %s\n" +
                                    "变电站: %s\n" +
                                    "设备编号: %s\n" +
                                    "设备名称: %s\n" +
                                    "设备类型: %s\n" +
                                    "告警类型: %s\n" +
                                    "告警等级: %s\n" +
                                    "当前值: %s\n" +
                                    "阈值: %s\n" +
                                    "持续时间: %s\n" +
                                    "请按照排查计划执行诊断，并输出结构化诊断报告。",
                            alarmTask.getAlarmId(),
                            alarmTask.getStation(),
                            alarmTask.getDeviceId(),
                            alarmTask.getDeviceName(),
                            alarmTask.getDeviceType(),
                            alarmTask.getAlarmType(),
                            alarmTask.getAlarmLevel(),
                            alarmTask.getCurrentValue(),
                            alarmTask.getThreshold(),
                            alarmTask.getDuration()
                    );

                    alarmTask.setStatus("DIAGNOSING");
                    alarmTask.setUpdatedAt(LocalDateTime.now());
                    alarmTaskMapper.updateById(alarmTask);
                } else {
                    diagnosisInput = "请对以下告警进行诊断分析：\n" + alarmDescription +
                            "\n请按照排查计划执行诊断，并输出结构化诊断报告。";
                }

                String result = invokeGraphForDiagnosis(diagnosisInput, taskId);

                emitter.send(SseEmitter.event().name("message").data(result));

                if (alarmTask != null) {
                    alarmTask.setStatus("COMPLETED");
                    alarmTask.setDiagnosisResult(result);
                    alarmTask.setUpdatedAt(LocalDateTime.now());
                    alarmTaskMapper.updateById(alarmTask);
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                logger.error("诊断执行失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("诊断失败: " + e.getMessage()));
                } catch (IOException ex) {
                    logger.error("发送错误事件失败", ex);
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    private String invokeGraphForDiagnosis(String diagnosisInput, String taskId) {
        Map<String, Object> initialState = new LinkedHashMap<>();
        initialState.put("input", diagnosisInput);
        initialState.put("session_id", "alarm-" + (taskId != null ? taskId : UUID.randomUUID().toString().substring(0, 8)));
        initialState.put("task_id", taskId != null ? taskId : "TASK-" + UUID.randomUUID().toString().substring(0, 8));
        initialState.put("intent", "DIAGNOSIS");

        Optional<OverAllState> resultOpt = compiledGraph.invoke(initialState);
        String answer = resultOpt
                .map(state -> state.value("final_response").map(Object::toString).orElse("诊断失败，请重试"))
                .orElse("诊断失败，请重试");
        logger.info("Graph引擎告警诊断成功, taskId={}", taskId);
        return answer;
    }

    @GetMapping("/diagnose/{taskId}/status")
    public Map<String, Object> getDiagnosisStatus(@PathVariable String taskId) {
        AlarmTask alarmTask = alarmTaskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlarmTask>()
                        .eq(AlarmTask::getTaskId, taskId)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        if (alarmTask != null) {
            response.put("taskId", alarmTask.getTaskId());
            response.put("alarmId", alarmTask.getAlarmId());
            response.put("status", alarmTask.getStatus());
            response.put("deviceName", alarmTask.getDeviceName());
            response.put("alarmType", alarmTask.getAlarmType());
            if ("COMPLETED".equals(alarmTask.getStatus())) {
                response.put("diagnosisResult", alarmTask.getDiagnosisResult());
            }
            if ("FAILED".equals(alarmTask.getStatus())) {
                response.put("errorMessage", alarmTask.getErrorMessage());
            }
        } else {
            response.put("error", "未找到任务: " + taskId);
        }
        return response;
    }

    @PostMapping("/resume/{taskId}")
    public Map<String, Object> resumeFromCheckpoint(@PathVariable String taskId) {
        logger.info("断点恢复请求: taskId={}", taskId);
        try {
            Map<String, Object> checkpoint = checkpointService.restoreFromCheckpoint(taskId);
            if (checkpoint == null) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("error", "未找到任务检查点: " + taskId);
                return response;
            }
            String question = Optional.ofNullable(checkpoint.get(GraphStateKeys.INPUT))
                    .or(() -> Optional.ofNullable(checkpoint.get(GraphStateKeys.CLEANED_INPUT)))
                    .map(Object::toString)
                    .orElse(null);
            if (question == null || question.isBlank()) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("error", "检查点数据不完整，无法恢复");
                return response;
            }
            logger.info("从检查点恢复任务: taskId={}", taskId);
            String result = invokeGraphForDiagnosis(question, taskId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("taskId", taskId);
            response.put("result", result);
            return response;
        } catch (Exception e) {
            logger.error("断点恢复失败: taskId={}", taskId, e);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("error", "断点恢复失败: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/checkpoint/{taskId}")
    public Map<String, Object> getCheckpoints(@PathVariable String taskId) {
        List<CheckpointRecord> checkpoints = checkpointService.getAllCheckpoints(taskId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("taskId", taskId);
        response.put("checkpointCount", checkpoints.size());
        response.put("checkpoints", checkpoints);
        return response;
    }
}
