package com.infosys.loan_service.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private Integer custId;
    private String name;
    private String email;
    private String phone;
    private String kycStatus;
}