package com.infosys.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;
    private Integer fromAccId;
    private Integer toAccId;
    private Integer beneficiaryId; // nullable
    private Double amount;
    private String method; // IMPS, NEFT, UPI
    private String status; // PENDING, SUCCESS, FAILED, FRAUD_BLOCKED
    private Integer fraudScore;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String utr; // Unique Transaction Reference, like real bank transfers
    private String settlementStatus; // PENDING, SETTLED
}