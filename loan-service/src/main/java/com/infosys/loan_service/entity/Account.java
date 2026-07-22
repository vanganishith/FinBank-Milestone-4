package com.infosys.loan_service.entity;

import lombok.*;

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