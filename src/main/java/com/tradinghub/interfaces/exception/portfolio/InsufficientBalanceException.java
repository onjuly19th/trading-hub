package com.tradinghub.interfaces.exception.portfolio;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 사용자의 잔액이 불충분하여 거래를 실행할 수 없을 때 발생하는 예외
 * 
 * 이 예외는 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 주문 금액이 사용자의 USD 잔액보다 클 때
 * 2. 출금 금액이 사용자의 가용 잔액보다 클 때
 * 3. 주문 수수료를 포함한 총액이 잔액을 초과할 때
 * 
 * HTTP 상태 코드: {@link HttpStatus#BAD_REQUEST} (400)
 * 에러 코드: {@link ErrorCodes.Portfolio#INSUFFICIENT_BALANCE}
 */
public class InsufficientBalanceException extends BusinessException {
    private static final String MESSAGE = "Insufficient balance";
    
    /**
     * 기본 메시지로 잔액 부족 예외 생성
     */
    public InsufficientBalanceException() {
        super(MESSAGE, ErrorCodes.Portfolio.INSUFFICIENT_BALANCE, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 상세 메시지로 잔액 부족 예외 생성
     * 
     * @param message 상세 에러 메시지 (필요한 금액 및 가용 금액 정보 포함 가능)
     */
    public InsufficientBalanceException(String message) {
        super(message, ErrorCodes.Portfolio.INSUFFICIENT_BALANCE, HttpStatus.BAD_REQUEST);
    }
} 