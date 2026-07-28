package com.infosys.transaction_service.exception;

public class SagaExecutionException extends RuntimeException {
    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}