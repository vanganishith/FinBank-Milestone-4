package com.infosys.loan_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionItem {
    private Integer repaymentId;
    private Integer loanId;
    private Integer custId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private Double amount;
    private long daysOverdue; // negative = not yet due, 0+ = overdue by that many days
    private String bucket;    // UPCOMING, DUE_TODAY, OVERDUE, SERIOUSLY_OVERDUE (90+)
}