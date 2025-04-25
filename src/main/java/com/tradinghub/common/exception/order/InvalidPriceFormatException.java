package com.tradinghub.common.exception.order;

public class InvalidPriceFormatException extends RuntimeException {
    public InvalidPriceFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}