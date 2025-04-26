package com.tradinghub.infrastructure.exception;

public class InvalidPriceFormatException extends RuntimeException {
    public InvalidPriceFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}