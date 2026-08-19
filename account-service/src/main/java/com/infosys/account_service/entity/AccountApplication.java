package com.infosys.account_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;
    private Integer custId;
    private String requestedAccType; // SAVINGS, CURRENT
    private Double initialDeposit;
    private String documentsSubmitted; // simple text description/reference for this project
    private String status; // PENDING_REVIEW, APPROVED, REJECTED
    private String rejectionReason;
    private Integer createdAccId; // set once approved and the real account exists
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}