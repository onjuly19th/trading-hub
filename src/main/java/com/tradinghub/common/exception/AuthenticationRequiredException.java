package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationRequiredException extends BusinessException {
    private static final String ERROR_CODE = "AUTHENTICATION_REQUIRED";
    private static final String MESSAGE = "인증이 필요합니다";
    
    public AuthenticationRequiredException() {
        super(MESSAGE, ERROR_CODE, HttpStatus.UNAUTHORIZED);
    }
    
    public AuthenticationRequiredException(String message) {
        super(message, ERROR_CODE, HttpStatus.UNAUTHORIZED);
    }
} 