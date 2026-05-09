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
@TableName("knowledge_chunk")
public class KnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chunkId;
    private String documentId;
    private Integer chunkIndex;
    private String chapter;
    private Integer pageNo;
    private String content;
    private Integer tokenCount;
    private String status;
    private LocalDateTime createdAt;
}
