package com.infosys.account_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private Integer accId;
    private String type;
    private Double amount;
    private String timestamp;
}