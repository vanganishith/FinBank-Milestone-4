package com.infosys.transaction_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private Integer accId;
    private String type;   // DEPOSIT or WITHDRAW
    private Double amount;
    private String timestamp;
}