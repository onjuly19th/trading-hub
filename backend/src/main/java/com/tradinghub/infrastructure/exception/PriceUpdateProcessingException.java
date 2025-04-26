package com.tradinghub.infrastructure.exception;

public class PriceUpdateProcessingException extends RuntimeException {
    public PriceUpdateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}