package com.infosys.loan_service.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovedEvent {
    private Integer loanId;
    private Integer accId;
    private Double principal;
    private String timestamp;
}