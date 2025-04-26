package com.tradinghub.interfaces.exception.order;

public class InvalidPriceFormatException extends RuntimeException {
    public InvalidPriceFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}