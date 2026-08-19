package com.infosys.account_service.dto;

import lombok.Data;

@Data
public class AccountApplicationRequest {
    private Integer custId;
    private String requestedAccType;
    private Double initialDeposit;
    private String documentsSubmitted;
}