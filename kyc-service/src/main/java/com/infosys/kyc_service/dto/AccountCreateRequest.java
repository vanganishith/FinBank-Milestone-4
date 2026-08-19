package com.infosys.kyc_service.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequest {
    private Integer custId;
    private String accType;
    private Double balance;
}