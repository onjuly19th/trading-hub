package com.tradinghub.interfaces.exception.portfolio;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 요청한 포트폴리오를 찾을 수 없을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 존재하지 않는 사용자 ID로 포트폴리오를 조회할 때
 * 2. 사용자가 아직 포트폴리오를 생성하지 않았을 때
 * 3. 포트폴리오가 삭제된 경우
 * 
 * HTTP 상태 코드: {@link HttpStatus#NOT_FOUND} (404)
 * 에러 코드: {@link ErrorCodes.Portfolio#PORTFOLIO_NOT_FOUND}
 */
public class PortfolioNotFoundException extends BusinessException {
    
    /**
     * 기본 메시지로 포트폴리오를 찾을 수 없음 예외 생성
     */
    public PortfolioNotFoundException() {
        super("Portfolio not found", ErrorCodes.Portfolio.PORTFOLIO_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 상세 메시지로 포트폴리오를 찾을 수 없음 예외 생성
     * 
     * @param message 상세 에러 메시지
     */
    public PortfolioNotFoundException(String message) {
        super(message, ErrorCodes.Portfolio.PORTFOLIO_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
} 