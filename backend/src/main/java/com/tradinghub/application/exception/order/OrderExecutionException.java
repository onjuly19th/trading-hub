package com.tradinghub.application.exception.order;

import org.springframework.http.HttpStatus;

import com.tradinghub.application.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 주문 실행 과정에서 오류가 발생했을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 주문 체결 중 데이터베이스 오류가 발생한 경우
 * 2. 외부 시스템과의 통신 오류가 발생한 경우
 * 3. 주문 실행에 필요한 자원에 접근할 수 없는 경우
 *
 * HTTP 상태 코드: {@link HttpStatus#INTERNAL_SERVER_ERROR} (500)
 * 에러 코드: {@link ErrorCodes.Order#ORDER_EXECUTION_ERROR}
 */
public class OrderExecutionException extends BusinessException {
    
    /**
     * 주문 실행 실패 예외 생성
     * 
     * @param message 상세 에러 메시지
     */
    public OrderExecutionException(String message) {
        super(message, ErrorCodes.Order.ORDER_EXECUTION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 주문 실행 실패 예외 생성
     * 
     * @param message 상세 에러 메시지
     * @param cause 원인 예외
     */
    public OrderExecutionException(String message, Throwable cause) {
        super(message, ErrorCodes.Order.ORDER_EXECUTION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
} 