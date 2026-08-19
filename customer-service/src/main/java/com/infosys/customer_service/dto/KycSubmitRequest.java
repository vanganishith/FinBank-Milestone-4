package com.infosys.customer_service.dto;

import lombok.Data;

@Data
public class KycSubmitRequest {
    private String accType; // SAVINGS or CURRENT
}