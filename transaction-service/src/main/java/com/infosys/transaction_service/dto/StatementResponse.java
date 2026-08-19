package com.infosys.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponse {
    private Integer accId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Double openingBalance;
    private Double closingBalance;
    private List<StatementLine> lines;
}