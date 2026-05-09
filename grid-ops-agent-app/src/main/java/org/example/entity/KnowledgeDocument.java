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
@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String documentId;
    private String documentName;
    private String documentType;
    private String source;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private Integer embeddingCount;
    private String description;
    private String tags;
    private Integer version;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
