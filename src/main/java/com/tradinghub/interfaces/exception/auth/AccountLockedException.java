package com.tradinghub.interfaces.exception.auth;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 잠긴 계정으로 로그인을 시도할 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 연속된 로그인 실패로 계정이 잠긴 경우
 * 2. 관리자에 의해 계정이 잠긴 경우
 * 
 * HTTP 상태 코드: {@link HttpStatus#FORBIDDEN} (403)
 * 에러 코드: {@link ErrorCodes.Auth#ACCOUNT_LOCKED}
 */
public class AccountLockedException extends BusinessException {
    
    /**
     * @param username 잠긴 계정의 사용자명
     */
    public AccountLockedException(String username) {
        super(
            String.format("Account '%s' is locked", username),
            ErrorCodes.Auth.ACCOUNT_LOCKED,
            HttpStatus.FORBIDDEN
        );
    }
} 