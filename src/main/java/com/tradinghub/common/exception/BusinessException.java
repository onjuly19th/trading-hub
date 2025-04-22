package com.tradinghub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 실행 중 발생하는 모든 예외의 기본 클래스
 * 예외 코드, HTTP 상태 코드, 메시지를 포함.
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    /**
     * 애플리케이션에서 정의한 에러 코드
     */
    private final String errorCode;
    
    /**
     * 응답에 사용될 HTTP 상태 코드
     */
    private final HttpStatus status;

    /**
     * 비즈니스 예외 생성
     * 
     * @param message 클라이언트에게 반환될 에러 메시지
     * @param errorCode 애플리케이션에서 정의한 에러 코드
     * @param status HTTP 응답 상태 코드
     */
    protected BusinessException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
} 