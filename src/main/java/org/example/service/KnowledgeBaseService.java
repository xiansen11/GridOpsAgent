package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import org.example.config.FileUploadConfig;
import org.example.constant.MilvusConstants;
import org.example.dto.DocumentChunk;
import org.example.entity.KnowledgeChunk;
import org.example.entity.KnowledgeDocument;
import org.example.entity.KnowledgeProcessTask;
import org.example.mapper.KnowledgeChunkMapper;
import org.example.mapper.KnowledgeDocumentMapper;
import org.example.mapper.KnowledgeProcessTaskMapper;
import org.example.service.parser.DocumentParser;
import org.example.service.parser.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private KnowledgeProcessTaskMapper processTaskMapper;

    @Autowired
    private KnowledgeChunkMapper chunkMapper;

    @Autowired
    private List<DocumentParser> documentParsers;

    @Autowired
    private DocumentChunkService chunkService;

    @Autowired
    private VectorEmbeddingService embeddingService;

    @Autowired
    private VectorIndexService indexService;

    @Autowired
    private VectorSearchService searchService;

    @Autowired
    private InMemoryVectorStore inMemoryVectorStore;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${milvus.enabled:false}")
    private boolean milvusEnabled;

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    public KnowledgeDocument uploadDocument(MultipartFile file, String documentType, String source, String description, String tags) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);

        validateFileType(fileType);

        String documentId = "DOC-" + UUID.randomUUID().toString().substring(0, 8);

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String filePath = uploadPath + File.separator + documentId + "." + fileType;
        file.transferTo(new File(filePath));

        int version = 1;
        KnowledgeDocument existingDoc = documentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getDocumentName, originalFilename)
                        .orderByDesc(KnowledgeDocument::getVersion)
                        .last("LIMIT 1"));
        if (existingDoc != null) {
            version = existingDoc.getVersion() + 1;
            existingDoc.setEnabled(0);
            existingDoc.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(existingDoc);
        }

        KnowledgeDocument document = KnowledgeDocument.builder()
                .documentId(documentId)
                .documentName(originalFilename)
                .documentType(documentType != null ? documentType : "未分类")
                .source(source)
                .filePath(filePath)
                .fileType(fileType)
                .fileSize(file.getSize())
                .status("UPLOADED")
                .chunkCount(0)
                .embeddingCount(0)
                .description(description)
                .tags(tags)
                .version(version)
                .enabled(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        documentMapper.insert(document);

        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8);
        KnowledgeProcessTask processTask = KnowledgeProcessTask.builder()
                .taskId(taskId)
                .documentId(documentId)
                .taskType("INDEX")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        processTaskMapper.insert(processTask);

        asyncExecutor.execute(() -> processDocument(documentId, taskId));

        return document;
    }

    private void processDocument(String documentId, String taskId) {
        try {
            updateProcessTask(taskId, "PARSING", "解析文档", null);

            KnowledgeDocument document = documentMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getDocumentId, documentId));
            if (document == null) {
                updateProcessTask(taskId, "FAILED", null, "文档不存在");
                return;
            }

            DocumentParser parser = documentParsers.stream()
                    .filter(p -> p.supports(document.getFileType()))
                    .findFirst()
                    .orElse(null);

            if (parser == null) {
                updateProcessTask(taskId, "FAILED", null, "不支持的文件类型: " + document.getFileType());
                return;
            }

            ParsedDocument parsedDoc;
            try (FileInputStream fis = new FileInputStream(document.getFilePath())) {
                parsedDoc = parser.parse(fis, document.getDocumentName());
            }

            updateProcessTask(taskId, "CLEANING", "清洗文本", null);

            String cleanedContent = cleanText(parsedDoc.getContent());

            updateProcessTask(taskId, "CHUNKING", "智能切片", null);

            List<DocumentChunk> chunks = chunkService.chunkDocument(cleanedContent, document.getFilePath());

            updateDocumentStatus(documentId, "CHUNKED", chunks.size());

            persistChunks(documentId, chunks);

            updateProcessTask(taskId, "INDEXING", "写入向量库", null);

            int embeddingCount = 0;
            for (DocumentChunk chunk : chunks) {
                try {
                    List<Float> embedding = embeddingService.generateEmbedding(chunk.getContent());

                    Map<String, Object> metadata = buildChunkMetadata(document, chunk, chunks.size());

                    if (milvusEnabled && milvusClient != null) {
                        insertToMilvus(chunk.getContent(), embedding, metadata, chunk.getChunkIndex());
                    } else {
                        insertToMemory(chunk.getContent(), embedding, metadata, chunk.getChunkIndex());
                    }

                    embeddingCount++;
                } catch (Exception e) {
                    logger.warn("向量化切片失败: chunkIndex={}, error={}", chunk.getChunkIndex(), e.getMessage());
                }
            }

            updateDocumentStatus(documentId, "COMPLETED", chunks.size());
            updateDocumentEmbeddingCount(documentId, embeddingCount);

            updateProcessTask(taskId, "COMPLETED", null, null);
            updateProcessTaskCompletedAt(taskId);

            logger.info("文档处理完成: documentId={}, chunks={}, embeddings={}", documentId, chunks.size(), embeddingCount);

        } catch (Exception e) {
            logger.error("文档处理失败: documentId={}", documentId, e);
            updateProcessTask(taskId, "FAILED", null, e.getMessage());
            updateDocumentStatus(documentId, "FAILED", 0);
        }
    }

    private void persistChunks(String documentId, List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            try {
                KnowledgeChunk knowledgeChunk = KnowledgeChunk.builder()
                        .chunkId(documentId + "-CHUNK-" + chunk.getChunkIndex())
                        .documentId(documentId)
                        .chunkIndex(chunk.getChunkIndex())
                        .chapter(chunk.getTitle())
                        .content(chunk.getContent())
                        .tokenCount(chunk.getContent().length() / 2)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                chunkMapper.insert(knowledgeChunk);
            } catch (Exception e) {
                logger.warn("持久化切片失败: chunkIndex={}, error={}", chunk.getChunkIndex(), e.getMessage());
            }
        }
        logger.info("切片持久化完成: documentId={}, count={}", documentId, chunks.size());
    }

    private Map<String, Object> buildChunkMetadata(KnowledgeDocument document, DocumentChunk chunk, int totalChunks) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", document.getDocumentId());
        metadata.put("documentName", document.getDocumentName());
        metadata.put("documentType", document.getDocumentType());
        metadata.put("source", document.getSource());
        metadata.put("_source", document.getFilePath());
        metadata.put("_file_name", document.getDocumentName());
        metadata.put("_extension", "." + document.getFileType());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("totalChunks", totalChunks);
        if (chunk.getTitle() != null && !chunk.getTitle().isEmpty()) {
            metadata.put("chapter", chunk.getTitle());
        }
        return metadata;
    }

    private void insertToMemory(String content, List<Float> vector,
                                Map<String, Object> metadata, int chunkIndex) {
        String source = (String) metadata.get("_source");
        String id = UUID.nameUUIDFromBytes((source + "_" + chunkIndex).getBytes()).toString();
        inMemoryVectorStore.insert(id, content, vector, metadata);
        logger.debug("向量插入内存存储成功: id={}, documentId={}, chunk={}", id, metadata.get("documentId"), chunkIndex);
    }

    private void insertToMilvus(String content, List<Float> vector,
                                Map<String, Object> metadata, int chunkIndex) throws Exception {
        try {
            R<RpcStatus> loadResponse = milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                            .build()
            );

            if (loadResponse.getStatus() != 0 && loadResponse.getStatus() != 65535) {
                throw new RuntimeException("加载 collection 失败: " + loadResponse.getMessage());
            }

            String source = (String) metadata.get("_source");
            String id = UUID.nameUUIDFromBytes((source + "_" + chunkIndex).getBytes()).toString();

            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("id", Collections.singletonList(id)));
            fields.add(new InsertParam.Field("content", Collections.singletonList(content)));
            fields.add(new InsertParam.Field("vector", Collections.singletonList(vector)));

            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject metadataJson = gson.toJsonTree(metadata).getAsJsonObject();
            fields.add(new InsertParam.Field("metadata", Collections.singletonList(metadataJson)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            R<MutationResult> insertResponse = milvusClient.insert(insertParam);

            if (insertResponse.getStatus() != 0) {
                throw new RuntimeException("插入向量失败: " + insertResponse.getMessage());
            }

            logger.debug("向量插入Milvus成功: id={}, documentId={}, chunk={}", id, metadata.get("documentId"), chunkIndex);

        } catch (Exception e) {
            logger.error("插入向量到 Milvus 失败", e);
            throw e;
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";

        text = text.replaceAll("\\u00A0", " ");
        text = text.replaceAll("[\\r\\t]", " ");
        text = text.replaceAll(" +", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.trim();

        return text;
    }

    public List<KnowledgeDocument> listDocuments(String documentType, int page, int size) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        if (documentType != null && !documentType.isEmpty()) {
            wrapper.eq(KnowledgeDocument::getDocumentType, documentType);
        }
        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);
        wrapper.last("LIMIT " + (page - 1) * size + "," + size);
        return documentMapper.selectList(wrapper);
    }

    public KnowledgeDocument getDocument(String documentId) {
        return documentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getDocumentId, documentId));
    }

    public KnowledgeProcessTask getProcessStatus(String documentId) {
        return processTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeProcessTask>()
                        .eq(KnowledgeProcessTask::getDocumentId, documentId)
                        .orderByDesc(KnowledgeProcessTask::getCreatedAt)
                        .last("LIMIT 1"));
    }

    public boolean deleteDocument(String documentId) {
        KnowledgeDocument document = getDocument(documentId);
        if (document == null) return false;

        if (milvusEnabled && milvusClient != null) {
            deleteFromMilvusByDocumentId(documentId);
        } else {
            inMemoryVectorStore.deleteByDocumentId(documentId);
        }

        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId));

        processTaskMapper.delete(new LambdaQueryWrapper<KnowledgeProcessTask>()
                .eq(KnowledgeProcessTask::getDocumentId, documentId));
        documentMapper.delete(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getDocumentId, documentId));

        if (document.getFilePath() != null) {
            File file = new File(document.getFilePath());
            if (file.exists()) file.delete();
        }

        logger.info("文档已删除: documentId={}", documentId);
        return true;
    }

    private void deleteFromMilvusByDocumentId(String documentId) {
        try {
            String expr = String.format("metadata[\"documentId\"] == \"%s\"", documentId);
            logger.info("从Milvus删除文档向量: documentId={}, expr={}", documentId, expr);

            R<RpcStatus> loadResponse = milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                            .build()
            );

            if (loadResponse.getStatus() != 0 && loadResponse.getStatus() != 65535) {
                logger.warn("加载 collection 失败: {}", loadResponse.getMessage());
                return;
            }

            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                    .withExpr(expr)
                    .build();

            R<MutationResult> response = milvusClient.delete(deleteParam);

            if (response.getStatus() != 0) {
                logger.warn("从Milvus删除文档向量时出现警告: {}", response.getMessage());
            } else {
                long deletedCount = response.getData().getDeleteCnt();
                logger.info("已从Milvus删除文档向量: documentId={}, 删除记录数: {}", documentId, deletedCount);
            }

        } catch (Exception e) {
            logger.warn("从Milvus删除文档向量失败: {}", e.getMessage());
        }
    }

    public void rollbackDocument(String documentId) {
        KnowledgeDocument targetDoc = getDocument(documentId);
        if (targetDoc == null) {
            throw new IllegalArgumentException("文档不存在: " + documentId);
        }

        List<KnowledgeDocument> allVersions = documentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getDocumentName, targetDoc.getDocumentName()));

        for (KnowledgeDocument v : allVersions) {
            if (v.getDocumentId().equals(documentId)) {
                v.setEnabled(1);
            } else {
                v.setEnabled(0);
            }
            v.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(v);
        }

        logger.info("文档版本回滚完成: documentId={}, version={}", documentId, targetDoc.getVersion());
    }

    public List<VectorSearchService.SearchResult> searchTest(String query, int topK) {
        return searchService.searchSimilarDocuments(query, topK);
    }

    private void validateFileType(String fileType) {
        String allowedExtensions = fileUploadConfig.getAllowedExtensions();
        if (allowedExtensions != null && !allowedExtensions.isEmpty()) {
            Set<String> allowed = Set.of(allowedExtensions.split(","));
            if (!allowed.contains(fileType)) {
                throw new IllegalArgumentException("不支持的文件类型: " + fileType + "，允许的类型: " + allowedExtensions);
            }
        }
    }

    private void updateProcessTask(String taskId, String status, String currentStep, String errorMessage) {
        KnowledgeProcessTask task = processTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeProcessTask>().eq(KnowledgeProcessTask::getTaskId, taskId));
        if (task != null) {
            task.setStatus(status);
            if (currentStep != null) task.setCurrentStep(currentStep);
            if (errorMessage != null) task.setErrorMessage(errorMessage);
            if ("PARSING".equals(status) && task.getStartedAt() == null) {
                task.setStartedAt(LocalDateTime.now());
            }
            processTaskMapper.updateById(task);
        }
    }

    private void updateProcessTaskCompletedAt(String taskId) {
        KnowledgeProcessTask task = processTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeProcessTask>().eq(KnowledgeProcessTask::getTaskId, taskId));
        if (task != null) {
            task.setCompletedAt(LocalDateTime.now());
            processTaskMapper.updateById(task);
        }
    }

    private void updateDocumentStatus(String documentId, String status, int chunkCount) {
        KnowledgeDocument document = documentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getDocumentId, documentId));
        if (document != null) {
            document.setStatus(status);
            document.setChunkCount(chunkCount);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
        }
    }

    private void updateDocumentEmbeddingCount(String documentId, int count) {
        KnowledgeDocument document = documentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getDocumentId, documentId));
        if (document != null) {
            document.setEmbeddingCount(count);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    @PreDestroy
    public void shutdown() {
        asyncExecutor.shutdown();
    }
}
