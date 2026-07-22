package com.infosys.loan_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer repaymentId;
    private Integer loanId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private Double amount;
    private String status; // PENDING, PAID, OVERDUE
}