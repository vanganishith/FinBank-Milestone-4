package com.infosys.customer_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAuthInternal {
    private Integer custId;
    private String username;
    private String passwordHash;
    private String kycStatus;
}