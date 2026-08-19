package com.infosys.kyc_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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