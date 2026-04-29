package org.example.security;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApprovalService {

    private final Map<String, ApprovalRequest> pendingApprovals = new ConcurrentHashMap<>();

    public ApprovalRequest createApprovalRequest(String taskId, String operation, String reason, String requestedBy) {
        String approvalId = "APR-" + UUID.randomUUID().toString().substring(0, 8);
        ApprovalRequest request = ApprovalRequest.builder()
                .approvalId(approvalId)
                .taskId(taskId)
                .operation(operation)
                .reason(reason)
                .requestedBy(requestedBy)
                .status("PENDING")
                .createdAt(new Date())
                .build();
        pendingApprovals.put(approvalId, request);
        return request;
    }

    public ApprovalRequest approve(String approvalId, String approvedBy, String comment) {
        ApprovalRequest request = pendingApprovals.get(approvalId);
        if (request != null && "PENDING".equals(request.getStatus())) {
            request.setStatus("APPROVED");
            request.setApprovedBy(approvedBy);
            request.setComment(comment);
            request.setProcessedAt(new Date());
        }
        return request;
    }

    public ApprovalRequest reject(String approvalId, String rejectedBy, String comment) {
        ApprovalRequest request = pendingApprovals.get(approvalId);
        if (request != null && "PENDING".equals(request.getStatus())) {
            request.setStatus("REJECTED");
            request.setApprovedBy(rejectedBy);
            request.setComment(comment);
            request.setProcessedAt(new Date());
        }
        return request;
    }

    public ApprovalRequest getApproval(String approvalId) {
        return pendingApprovals.get(approvalId);
    }

    public List<ApprovalRequest> getPendingApprovals() {
        return pendingApprovals.values().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .toList();
    }

    public boolean isApproved(String approvalId) {
        ApprovalRequest request = pendingApprovals.get(approvalId);
        return request != null && "APPROVED".equals(request.getStatus());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApprovalRequest {
        private String approvalId;
        private String taskId;
        private String operation;
        private String reason;
        private String requestedBy;
        private String status;
        private String approvedBy;
        private String comment;
        private Date createdAt;
        private Date processedAt;
    }
}
