package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class PortfolioNotFoundException extends BusinessException {
    private static final String ERROR_CODE = "PORTFOLIO_NOT_FOUND";
    private static final String MESSAGE = "포트폴리오를 찾을 수 없습니다";
    
    public PortfolioNotFoundException() {
        super(MESSAGE, ERROR_CODE, HttpStatus.NOT_FOUND);
    }
    
    public PortfolioNotFoundException(String message) {
        super(message, ERROR_CODE, HttpStatus.NOT_FOUND);
    }
} 