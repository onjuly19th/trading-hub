package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends BusinessException {
    private static final String ERROR_CODE = "INSUFFICIENT_BALANCE";
    private static final String MESSAGE = "잔액이 부족합니다";
    
    public InsufficientBalanceException() {
        super(MESSAGE, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }
    
    public InsufficientBalanceException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }
} 