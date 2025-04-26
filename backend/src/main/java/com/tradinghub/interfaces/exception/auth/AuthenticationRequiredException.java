package com.tradinghub.interfaces.exception.auth;

import org.springframework.http.HttpStatus;

import com.tradinghub.application.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 인증이 필요한 리소스에 인증 없이 접근하려고 할 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 로그인 없이 인증이 필요한 API를 호출할 때
 * 2. 인증 토큰이 만료되었을 때
 * 3. 유효하지 않은 인증 정보로 요청할 때
 * 
 * HTTP 상태 코드: {@link HttpStatus#UNAUTHORIZED} (401) 
 * 에러 코드: {@link ErrorCodes.Auth#AUTHENTICATION_REQUIRED}
 */
public class AuthenticationRequiredException extends BusinessException {
    private static final String MESSAGE = "Authentication required";
    
    /**
     * 기본 메시지로 인증 필요 예외 생성
     */
    public AuthenticationRequiredException() {
        super(MESSAGE, ErrorCodes.Auth.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * 상세 메시지로 인증 필요 예외 생성
     * 
     * @param message 상세 에러 메시지
     */
    public AuthenticationRequiredException(String message) {
        super(message, ErrorCodes.Auth.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED);
    }
} 