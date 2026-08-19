package com.infosys.loan_service.dto;

import lombok.*;
import com.infosys.loan_service.entity.Loan;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditProfile {
    private Integer custId;
    private String customerName;
    private String kycStatus;
    private Double accountBalance;
    private String accountStatus;
    private List<Loan> existingLoans;
    private long activeLoanCount;
    private long npaLoanCount;
    private String suggestion; // simple hint text for the teller
}