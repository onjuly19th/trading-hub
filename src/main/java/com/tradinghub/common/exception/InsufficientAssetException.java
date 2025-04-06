package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientAssetException extends BusinessException {
    private static final String ERROR_CODE = "INSUFFICIENT_ASSET";
    private static final String MESSAGE = "자산이 부족합니다";
    
    public InsufficientAssetException() {
        super(MESSAGE, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }
    
    public InsufficientAssetException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }
} 