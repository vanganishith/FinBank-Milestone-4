package com.infosys.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementLine {
    private Integer txnId;
    private String type;
    private Double amount;
    private LocalDateTime txnDate;
    private Double runningBalance;
}