package com.infosys.transaction_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private Integer accId;
    private Integer custId;
    private String accType;
    private Double balance;
    private String status;
}