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
@TableName("alarm_task")
public class AlarmTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String alarmId;
    private String station;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String alarmType;
    private String alarmLevel;
    private String alarmSource;
    private String currentValue;
    private String threshold;
    private String duration;
    private String status;
    private String selectedSkill;
    private String diagnosisResult;
    private String checkpointData;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
