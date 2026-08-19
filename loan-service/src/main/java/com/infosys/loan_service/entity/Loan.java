package com.infosys.loan_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer loanId;
    private Integer custId;
    private Integer accId;
    private Double principal;
    private Double interestRate;   // annual %, e.g. 8.5
    private Integer tenureMonths;
    private Double emi;
    private Integer creditScore;
    private String status;         // PENDING, APPROVED, REJECTED, ACTIVE, CLOSED, NPA
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime disbursedAt;
}