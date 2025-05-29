package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;

import com.tradinghub.application.dto.UpdatePortfolioCommand;
import com.tradinghub.domain.model.portfolio.Portfolio;

/**
 * 주문에 대한 포트폴리오 처리 인터페이스
 * 매수/매도 주문에 대한 공통 처리 로직을 정의합니다.
 */
public interface PortfolioOrderHandler {
    
    /**
     * 해당 주문 타입을 지원하는지 확인합니다.
     * 
     * @param request 주문 실행 요청
     * @return 지원 여부
     */
    boolean supports(UpdatePortfolioCommand command);
    
    /**
     * 주문을 처리합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param command 주문 실행 요청
     * @param orderAmount 주문 금액
     */
    void processOrder(Portfolio portfolio, UpdatePortfolioCommand command, BigDecimal orderAmount);
}