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
@TableName("tool_call_log")
public class ToolCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String spanId;
    private String taskId;
    private String sessionId;
    private String toolName;
    private String toolSource;
    private String requestParam;
    private String responseData;
    private String status;
    private Long durationMs;
    private String errorType;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
}
