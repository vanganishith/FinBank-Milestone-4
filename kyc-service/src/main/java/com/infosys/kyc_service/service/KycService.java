package com.infosys.kyc_service.service;

import com.infosys.kyc_service.entity.*;
import com.infosys.kyc_service.exception.InvalidRequestException;
import com.infosys.kyc_service.feign.AccountFeignClient;
import com.infosys.kyc_service.feign.AccountCreateRequest;
import com.infosys.kyc_service.feign.CustomerFeignClient;
import com.infosys.kyc_service.repository.KycApplicationRepo;
import com.infosys.kyc_service.repository.KycAuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KycService {

    @Autowired
    KycApplicationRepo appRepo;
    @Autowired
    KycAuditLogRepo auditRepo;
    @Autowired
    CustomerFeignClient customerFeignClient;
    @Autowired
    KycSimulationService simService;
    @Autowired
    AccountFeignClient accountFeignClient;

    private static final int RISK_THRESHOLD = 40; // >= this routes to manager review

    private void log(Integer applicationId, String action, String details) {
        auditRepo.save(new KycAuditLog(null, applicationId, action, details, LocalDateTime.now()));
    }

    private void createAccountIfRequested(KycApplication app) {
        if (app.getRequestedAccType() == null) return;
        accountFeignClient.createAccount(new AccountCreateRequest(
            app.getCustId(), app.getRequestedAccType(), app.getInitialDeposit() != null ? app.getInitialDeposit() : 0.0
        ));
        log(app.getApplicationId(), "ACCOUNT_CREATED",
            "Account Service created a " + app.getRequestedAccType() + " account, initial deposit " + app.getInitialDeposit());
    }

    public KycApplication submit(Integer custId, String documentType, String documentReference,
        boolean documentUploaded, boolean selfieCaptured,
        String documentImageBase64, String selfieImageBase64, Boolean livenessMotionDetected,
        String supportingDocumentBase64, String requestedAccType, Double initialDeposit) {
        Customer customer = customerFeignClient.getCustomer(custId);
        if (customer == null) {
            throw new InvalidRequestException("Customer not found.");
        }
        if (documentReference == null || documentReference.isBlank()) {
            throw new InvalidRequestException("Document reference is required.");
        }
        if (!simService.validateDocumentNumber(documentType, documentReference)) {
            throw new InvalidRequestException("Aadhaar number must be exactly 12 digits.");
        }
        if (!documentUploaded || !selfieCaptured) {
            throw new InvalidRequestException("Submit a document and complete the live selfie capture before verification.");
        }
        if (documentImageBase64 == null || selfieImageBase64 == null) {
            throw new InvalidRequestException("Both a document photo and a live selfie are required.");
        }
        if (!Boolean.TRUE.equals(livenessMotionDetected)) {
            throw new InvalidRequestException("Liveness check failed — no blink/movement detected during capture.");
        }

        List<KycApplication> existing = appRepo.findByCustId(custId);
        boolean alreadyPending = existing.stream().anyMatch(a -> "PENDING_REVIEW".equals(a.getStatus()));
        if (alreadyPending) {
            throw new InvalidRequestException("You already have a KYC application pending manager review.");
        }

        KycApplication app = new KycApplication();
        app.setCustId(custId);
        app.setDocumentType(documentType);
        app.setDocumentReference(documentReference);
        app.setDocumentUploaded(true);
        app.setSelfieCaptured(true);
        app.setSubmittedAt(LocalDateTime.now());
        app.setRequestedAccType(requestedAccType);
        app.setInitialDeposit(initialDeposit);

        String ocrName = simService.simulateOcr(documentReference, customer.getName());
        boolean ocrVerified = ocrName != null;
        app.setOcrStatus(ocrVerified ? "VERIFIED" : "MISMATCH");
        app.setOcrExtractedName(ocrName);

        int faceScore = simService.simulateFaceMatchScore(documentImageBase64, selfieImageBase64);
        app.setFaceMatchScore(faceScore);

        boolean liveness = livenessMotionDetected;
        app.setLivenessPassed(liveness);
        boolean documentNumberValid = simService.validateDocumentNumber(documentType, documentReference);
        int riskScore = simService.calculateRiskScore(ocrVerified, documentNumberValid, faceScore, liveness);
        app.setRiskScore(riskScore);
        app.setRiskLevel(riskScore >= RISK_THRESHOLD ? "HIGH" : "LOW");
        app.setDocumentImageBase64(documentImageBase64);
        app.setSelfieImageBase64(selfieImageBase64);
        app.setSupportingDocumentBase64(supportingDocumentBase64);
        app.setLivenessMotionDetected(livenessMotionDetected);

        KycApplication saved = appRepo.save(app);
        log(saved.getApplicationId(), "SUBMITTED",
                "Doc: " + documentType + " uploaded | Live selfie captured | OCR: " + app.getOcrStatus() + " | Face: " + faceScore
                        + " | Liveness: " + liveness + " | Risk: " + riskScore);

        if (riskScore < RISK_THRESHOLD) {
            saved.setStatus("AUTO_CLEARED");
            saved.setReviewedAt(LocalDateTime.now());
            appRepo.save(saved);
            log(saved.getApplicationId(), "AUTO_CLEARED", "Risk score " + riskScore + " below threshold " + RISK_THRESHOLD);
            customerFeignClient.verifyKyc(custId);
            log(saved.getApplicationId(), "CUSTOMER_KYC_UPDATED", "Customer Service kycStatus set to VERIFIED");
            createAccountIfRequested(saved);
        } else {
            saved.setStatus("PENDING_REVIEW");
            appRepo.save(saved);
            log(saved.getApplicationId(), "ROUTED_TO_REVIEW", "Risk score " + riskScore + " at/above threshold " + RISK_THRESHOLD);
        }

        return saved;
    }

    public KycApplication approve(Integer applicationId) {
        KycApplication app = appRepo.findById(applicationId).orElseThrow(() -> new RuntimeException("Application not found"));
        if (!"PENDING_REVIEW".equals(app.getStatus())) {
            throw new InvalidRequestException("Application is not pending review. Current status: " + app.getStatus());
        }
        app.setStatus("APPROVED");
        app.setReviewedAt(LocalDateTime.now());
        appRepo.save(app);
        log(applicationId, "MANAGER_APPROVED", "Manager approved despite HIGH risk classification");
        customerFeignClient.verifyKyc(app.getCustId());
        log(applicationId, "CUSTOMER_KYC_UPDATED", "Customer Service kycStatus set to VERIFIED");
        createAccountIfRequested(app);
        return app;
    }

    public KycApplication reject(Integer applicationId, String reason) {
        KycApplication app = appRepo.findById(applicationId).orElseThrow(() -> new RuntimeException("Application not found"));
        if (!"PENDING_REVIEW".equals(app.getStatus())) {
            throw new InvalidRequestException("Application is not pending review. Current status: " + app.getStatus());
        }
        app.setStatus("REJECTED");
        app.setRejectionReason(reason != null ? reason : "Not specified");
        app.setReviewedAt(LocalDateTime.now());
        appRepo.save(app);
        log(applicationId, "MANAGER_REJECTED", "Reason: " + app.getRejectionReason());
        customerFeignClient.rejectKyc(app.getCustId(), app.getRejectionReason());
        log(applicationId, "CUSTOMER_KYC_UPDATED", "Customer Service kycStatus set to REJECTED");
        return app;
    }

    public List<KycApplication> getPendingReview() {
        return appRepo.findByStatus("PENDING_REVIEW");
    }

    public List<KycApplication> getByCustomer(Integer custId) {
        return appRepo.findByCustId(custId);
    }
}