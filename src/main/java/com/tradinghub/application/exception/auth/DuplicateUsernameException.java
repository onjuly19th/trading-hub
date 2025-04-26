package com.tradinghub.application.exception.auth;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 이미 존재하는 사용자명으로 회원가입을 시도할 때 발생하는 예외
 * 
 * HTTP 상태 코드: {@link HttpStatus#CONFLICT} (409)
 * 에러 코드: {@link ErrorCodes.Auth#DUPLICATE_USERNAME}
 */
public class DuplicateUsernameException extends BusinessException {
    
    /**
     * @param username 중복된 사용자명
     */
    public DuplicateUsernameException(String username) {
        super(
            String.format("Username '%s' is already taken", username),
            ErrorCodes.Auth.DUPLICATE_USERNAME,
            HttpStatus.CONFLICT
        );
    }
} 