package com.infosys.account_service.event;

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