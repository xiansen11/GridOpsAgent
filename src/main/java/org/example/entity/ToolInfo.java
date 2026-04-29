package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tool_registry")
public class ToolInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String toolId;
    private String toolName;
    private String description;
    private String inputSchema;
    private String outputSchema;
    private String tags;
    private String permissionLevel;
    private String riskLevel;
    private String ownerSystem;
    private Boolean enabled;
}
