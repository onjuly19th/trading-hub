package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends BusinessException {
    private static final String ERROR_CODE = "INSUFFICIENT_BALANCE";
    private static final String MESSAGE = "Insufficient balance";
    
    public InsufficientBalanceException() {
        super(MESSAGE, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }
    
    public InsufficientBalanceException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }
} 