package org.example.controller;

import org.example.security.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @PostMapping("/request")
    public Map<String, Object> createRequest(@RequestBody Map<String, String> request) {
        ApprovalService.ApprovalRequest approval = approvalService.createApprovalRequest(
                request.get("taskId"),
                request.get("operation"),
                request.get("reason"),
                request.getOrDefault("requestedBy", "system"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("approvalId", approval.getApprovalId());
        response.put("status", approval.getStatus());
        return response;
    }

    @PostMapping("/approve")
    public Map<String, Object> approve(@RequestBody Map<String, String> request) {
        ApprovalService.ApprovalRequest approval = approvalService.approve(
                request.get("approvalId"),
                request.getOrDefault("approvedBy", "admin"),
                request.get("comment"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("approvalId", approval.getApprovalId());
        response.put("status", approval.getStatus());
        return response;
    }

    @PostMapping("/reject")
    public Map<String, Object> reject(@RequestBody Map<String, String> request) {
        ApprovalService.ApprovalRequest approval = approvalService.reject(
                request.get("approvalId"),
                request.getOrDefault("rejectedBy", "admin"),
                request.get("comment"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("approvalId", approval.getApprovalId());
        response.put("status", approval.getStatus());
        return response;
    }

    @GetMapping("/pending")
    public List<ApprovalService.ApprovalRequest> getPending() {
        return approvalService.getPendingApprovals();
    }
}
