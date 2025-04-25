package com.tradinghub.common.exception.auth;

import org.springframework.http.HttpStatus;
import com.tradinghub.common.exception.BusinessException;
import com.tradinghub.common.exception.ErrorCodes;

/**
 * 인증 요청 데이터가 유효하지 않을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 필수 필드가 누락된 경우
 * 2. 필드 값이 유효하지 않은 경우 (예: 비밀번호 형식 불일치)
 * 
 * HTTP 상태 코드: {@link HttpStatus#BAD_REQUEST} (400)
 * 에러 코드: {@link ErrorCodes.Auth#INVALID_REQUEST}
 */
public class InvalidRequestException extends BusinessException {
    
    /**
     * @param message 유효성 검증 실패 사유
     */
    public InvalidRequestException(String message) {
        super(
            message,
            ErrorCodes.Auth.INVALID_REQUEST,
            HttpStatus.BAD_REQUEST
        );
    }
} 