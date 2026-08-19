package com.infosys.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private Integer paymentId;
    private Integer fromAccId;
    private Integer toAccId;
    private Double amount;
    private String timestamp;
}