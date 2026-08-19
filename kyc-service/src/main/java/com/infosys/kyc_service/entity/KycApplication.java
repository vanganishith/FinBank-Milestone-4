package com.infosys.kyc_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;
    private Integer custId;
    private String documentType; // AADHAAR, PAN, PASSPORT
    private String documentReference;
    private Boolean documentUploaded;
    private Boolean selfieCaptured;

    private String ocrStatus;       // VERIFIED, MISMATCH
    private String ocrExtractedName;
    private Integer faceMatchScore; // 0-100, simulated
    private Boolean livenessPassed; // simulated

    private Integer riskScore;      // 0-100
    private String riskLevel;       // LOW, HIGH

    private String status;          // PENDING_REVIEW, AUTO_CLEARED, APPROVED, REJECTED
    private String rejectionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    private String requestedAccType;   // SAVINGS or CURRENT, null if not requesting a new account
    private Double initialDeposit;

    @Column(length = 2000000)
    private String documentImageBase64;
    @Column(length = 2000000)
    private String selfieImageBase64;
    private Boolean livenessMotionDetected; // true = real blink/movement was captured client-side
    @Column(columnDefinition = "MEDIUMTEXT")
    private String supportingDocumentBase64;
}
