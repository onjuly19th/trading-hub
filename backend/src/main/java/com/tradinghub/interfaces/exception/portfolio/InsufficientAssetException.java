package com.tradinghub.interfaces.exception.portfolio;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 사용자의 자산(코인) 잔액이 불충분하여 거래를 실행할 수 없을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 매도하려는 코인 수량이 보유한 수량보다 클 때
 * 2. 출금하려는 코인 수량이 보유한 수량보다 클 때
 * 3. 송금하려는 코인 수량이 보유한 수량보다 클 때
 *
 * HTTP 상태 코드: {@link HttpStatus#BAD_REQUEST} (400)
 * 에러 코드: {@link ErrorCodes.Portfolio#INSUFFICIENT_ASSET}
 */
public class InsufficientAssetException extends BusinessException {
    private static final String MESSAGE = "Insufficient asset";
    
    /**
     * 기본 메시지로 자산 부족 예외 생성
     */
    public InsufficientAssetException() {
        super(MESSAGE, ErrorCodes.Portfolio.INSUFFICIENT_ASSET, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 상세 메시지로 자산 부족 예외 생성
     * 
     * @param message 상세 에러 메시지 (필요한 자산 수량 및 가용 수량 정보 포함 가능)
     */
    public InsufficientAssetException(String message) {
        super(message, ErrorCodes.Portfolio.INSUFFICIENT_ASSET, HttpStatus.BAD_REQUEST);
    }
} 