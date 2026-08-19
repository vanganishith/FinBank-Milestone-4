package com.infosys.customer_service.dto;

import lombok.Data;

@Data
public class ClaimRequest {
    private Integer custId;
    private String username;
    private String password;
}