package com.tradinghub.application.exception.auth;

import org.springframework.http.HttpStatus;

import com.tradinghub.application.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 인증 실패 시 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 잘못된 사용자명
 * 2. 잘못된 비밀번호
 * 
 * HTTP 상태 코드: {@link HttpStatus#UNAUTHORIZED} (401)
 * 에러 코드: {@link ErrorCodes.Auth#AUTHENTICATION_FAILED}
 */
public class AuthenticationFailedException extends BusinessException {
    
    /**
     * @param message 인증 실패 사유
     */
    public AuthenticationFailedException(String message) {
        super(
            message,
            ErrorCodes.Auth.AUTHENTICATION_FAILED,
            HttpStatus.UNAUTHORIZED
        );
    }
} 