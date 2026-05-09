package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_execution_log")
public class AgentExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String taskId;
    private String sessionId;
    private String nodeName;
    private String agentName;
    private String stepName;
    private String inputSummary;
    private String outputSummary;
    private String status;
    private Long durationMs;
    private Integer tokenCount;
    private String modelName;
    private String errorType;
    private String errorMessage;
    private LocalDateTime createdAt;
}
