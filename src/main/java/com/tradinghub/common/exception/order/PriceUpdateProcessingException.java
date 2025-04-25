package com.tradinghub.common.exception.order;

public class PriceUpdateProcessingException extends RuntimeException {
    public PriceUpdateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}