package com.tradinghub.interfaces.exception.order;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 주문 정보가 유효하지 않을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 주문 수량이 음수이거나 0인 경우
 * 2. 주문 가격이 음수인 경우
 * 3. 거래 가능하지 않은 코인 심볼이 지정된 경우
 * 4. 주문 타입이 올바르지 않은 경우
 * 
 * HTTP 상태 코드: {@link HttpStatus#BAD_REQUEST} (400
 * 에러 코드: {@link ErrorCodes.Order#INVALID_ORDER}
 */
public class InvalidOrderException extends BusinessException {
    
    /**
     * 유효하지 않은 주문 예외 생성
     * 
     * @param message 상세 에러 메시지
     */
    public InvalidOrderException(String message) {
        super(message, ErrorCodes.Order.INVALID_ORDER, HttpStatus.BAD_REQUEST);
    }
} 