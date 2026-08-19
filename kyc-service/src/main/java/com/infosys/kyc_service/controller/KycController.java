package com.infosys.kyc_service.controller;

import com.infosys.kyc_service.dto.KycSubmissionRequest;
import com.infosys.kyc_service.entity.KycApplication;
import com.infosys.kyc_service.entity.KycAuditLog;
import com.infosys.kyc_service.repository.KycAuditLogRepo;
import com.infosys.kyc_service.service.KycService;
import com.infosys.kyc_service.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/kyc")
public class KycController {

    @Autowired
    KycService kycService;
    @Autowired
    KycAuditLogRepo auditRepo;
    @Autowired
    AuthUtil authUtil;

    @PostMapping("/submit")
    public KycApplication submit(@RequestBody KycSubmissionRequest req,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireSelfOrTeller(authHeader, req.getCustId());
        return kycService.submit(req.getCustId(), req.getDocumentType(), req.getDocumentReference(),
                Boolean.TRUE.equals(req.getDocumentUploaded()), Boolean.TRUE.equals(req.getSelfieCaptured()),
                req.getDocumentImageBase64(), req.getSelfieImageBase64(), req.getLivenessMotionDetected(),
                req.getSupportingDocumentBase64(), req.getRequestedAccType(), req.getInitialDeposit());
    }
    @GetMapping("/pending-review")
    public List<KycApplication> pendingReview(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        return kycService.getPendingReview();
    }

    @PutMapping("/{applicationId}/approve")
    public KycApplication approve(@PathVariable Integer applicationId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        return kycService.approve(applicationId);
    }

    @PutMapping("/{applicationId}/reject")
    public KycApplication reject(@PathVariable Integer applicationId, @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        return kycService.reject(applicationId, reason);
    }

    @GetMapping("/customer/{custId}")
    public List<KycApplication> byCustomer(@PathVariable Integer custId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireSelfOrTeller(authHeader, custId);
        return kycService.getByCustomer(custId);
    }

    @GetMapping("/{applicationId}/audit-log")
    public List<KycAuditLog> auditLog(@PathVariable Integer applicationId) {
        return auditRepo.findByApplicationIdOrderByTimestampAsc(applicationId);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        List<KycApplication> all = new java.util.ArrayList<>();
        kycService.getPendingReview().forEach(all::add); // just for pending count below
        long pending = kycService.getPendingReview().size();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("pendingReview", pending);
        return stats;
    }
}
