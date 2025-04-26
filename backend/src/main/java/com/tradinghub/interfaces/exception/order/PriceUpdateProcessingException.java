package com.tradinghub.interfaces.exception.order;

public class PriceUpdateProcessingException extends RuntimeException {
    public PriceUpdateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}