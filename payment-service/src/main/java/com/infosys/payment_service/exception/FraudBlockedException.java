package com.infosys.payment_service.exception;

public class FraudBlockedException extends RuntimeException {
    public FraudBlockedException(String message) {
        super(message);
    }
}