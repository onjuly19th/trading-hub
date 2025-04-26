package com.tradinghub.interfaces.exception.auth;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 사용자가 권한이 없는 작업을 시도할 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 사용자가 권한이 없는 작업을 시도할 때
 * 2. 사용자가 필요한 권한을 가지고 있지 않을 때
 * 
 * HTTP 상태 코드: {@link HttpStatus#FORBIDDEN} (403)
 * 에러 코드: {@link ErrorCodes.Auth#UNAUTHORIZED_OPERATION}
 */
public class UnauthorizedOperationException extends BusinessException {
    
    /**
     * 권한 없는 작업 예외 생성
     * 
     * @param message 상세 에러 메시지
     */
    public UnauthorizedOperationException(String message) {
        super(message, ErrorCodes.Auth.UNAUTHORIZED_OPERATION, HttpStatus.FORBIDDEN);
    }
} 