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
@TableName("checkpoint_record")
public class CheckpointRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String stepName;
    private String agentState;
    private String planSteps;
    private String completedSteps;
    private String ragResults;
    private String toolResults;
    private String subagentResults;
    private String diagnosisDraft;
    private String approvalStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
}
