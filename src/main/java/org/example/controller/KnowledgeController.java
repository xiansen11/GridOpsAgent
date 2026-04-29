package org.example.controller;

import org.example.entity.KnowledgeDocument;
import org.example.entity.KnowledgeProcessTask;
import org.example.service.KnowledgeBaseService;
import org.example.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeController.class);

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/documents/upload")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags) {

        logger.info("上传文档: fileName={}, documentType={}, source={}", file.getOriginalFilename(), documentType, source);

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            KnowledgeDocument document = knowledgeBaseService.uploadDocument(file, documentType, source, description, tags);
            response.put("documentId", document.getDocumentId());
            response.put("documentName", document.getDocumentName());
            response.put("status", document.getStatus());
            response.put("message", "文档上传成功，正在后台处理");
        } catch (IllegalArgumentException e) {
            logger.warn("文档上传校验失败: {}", e.getMessage());
            response.put("error", e.getMessage());
        } catch (Exception e) {
            logger.error("文档上传失败", e);
            response.put("error", "上传失败: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/documents/{documentId}/status")
    public Map<String, Object> getDocumentStatus(@PathVariable String documentId) {
        Map<String, Object> response = new LinkedHashMap<>();

        KnowledgeDocument document = knowledgeBaseService.getDocument(documentId);
        if (document == null) {
            response.put("error", "文档不存在: " + documentId);
            return response;
        }

        response.put("documentId", document.getDocumentId());
        response.put("documentName", document.getDocumentName());
        response.put("status", document.getStatus());
        response.put("chunkCount", document.getChunkCount());
        response.put("embeddingCount", document.getEmbeddingCount());

        KnowledgeProcessTask processTask = knowledgeBaseService.getProcessStatus(documentId);
        if (processTask != null) {
            response.put("processStatus", processTask.getStatus());
            response.put("currentStep", processTask.getCurrentStep());
            response.put("errorMessage", processTask.getErrorMessage());
        }

        return response;
    }

    @GetMapping("/documents")
    public Map<String, Object> listDocuments(
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        List<KnowledgeDocument> documents = knowledgeBaseService.listDocuments(documentType, page, size);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("documents", documents);
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    @DeleteMapping("/documents/{documentId}")
    public Map<String, Object> deleteDocument(@PathVariable String documentId) {
        boolean deleted = knowledgeBaseService.deleteDocument(documentId);
        Map<String, Object> response = new LinkedHashMap<>();
        if (deleted) {
            response.put("message", "文档已删除: " + documentId);
        } else {
            response.put("error", "文档不存在: " + documentId);
        }
        return response;
    }

    @PostMapping("/search/test")
    public Map<String, Object> searchTest(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.containsKey("topK") ? (int) request.get("topK") : 5;

        List<VectorSearchService.SearchResult> results = knowledgeBaseService.searchTest(query, topK);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("results", results);
        response.put("total", results.size());
        return response;
    }

    @GetMapping("/documents/{documentName}/versions")
    public Map<String, Object> getDocumentVersions(@PathVariable String documentName) {
        List<KnowledgeDocument> versions = knowledgeBaseService.listDocuments(null, 1, 100)
                .stream()
                .filter(d -> d.getDocumentName().equals(documentName))
                .sorted((a, b) -> Integer.compare(b.getVersion(), a.getVersion()))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("documentName", documentName);
        response.put("versions", versions);
        response.put("totalVersions", versions.size());
        return response;
    }

    @PostMapping("/documents/{documentId}/rollback")
    public Map<String, Object> rollbackDocument(@PathVariable String documentId) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            knowledgeBaseService.rollbackDocument(documentId);
            KnowledgeDocument doc = knowledgeBaseService.getDocument(documentId);
            response.put("message", "已回滚到版本 " + (doc != null ? doc.getVersion() : "未知"));
            response.put("documentId", documentId);
            response.put("version", doc != null ? doc.getVersion() : null);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
        }
        return response;
    }
}
