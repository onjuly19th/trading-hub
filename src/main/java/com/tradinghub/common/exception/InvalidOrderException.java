package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderException extends BusinessException {
    public InvalidOrderException(String reason) {
        super("Invalid order: " + reason, "INVALID_ORDER", HttpStatus.BAD_REQUEST);
    }
} 