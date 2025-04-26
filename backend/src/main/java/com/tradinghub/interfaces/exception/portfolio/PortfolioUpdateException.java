package com.tradinghub.interfaces.exception.portfolio;

import org.springframework.http.HttpStatus;

import com.tradinghub.interfaces.exception.BusinessException;

/**
 * 포트폴리오 업데이트 중 발생하는 예외
 */
public class PortfolioUpdateException extends BusinessException {
    
    public PortfolioUpdateException(String message) {
        super("PORTFOLIO_UPDATE_FAILED", message, HttpStatus.INTERNAL_SERVER_ERROR);
    } 
} 