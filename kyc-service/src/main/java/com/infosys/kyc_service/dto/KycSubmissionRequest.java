package com.infosys.kyc_service.dto;

import lombok.Data;

@Data
public class KycSubmissionRequest {
    private Integer custId;
    private String documentType;
    private String documentReference;
    private Boolean documentUploaded;
    private Boolean selfieCaptured;
    private String documentImageBase64;
    private String selfieImageBase64;
    private Boolean livenessMotionDetected;
    private String supportingDocumentBase64; // optional, e.g. PDF — not used in face match
    private String requestedAccType;
    private Double initialDeposit;
}
