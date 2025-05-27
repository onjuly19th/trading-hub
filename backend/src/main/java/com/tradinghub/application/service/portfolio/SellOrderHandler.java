package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioAsset;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;
import com.tradinghub.interfaces.dto.order.OrderExecutionRequest;

import lombok.RequiredArgsConstructor;

/**
 * 매도 주문 처리 전략
 */
@Component
@RequiredArgsConstructor
public class SellOrderHandler implements PortfolioOrderHandler {
    
    private final PortfolioValidator portfolioValidator;
    private final PortfolioAssetManager assetManager;

    @Override
    public boolean supports(OrderExecutionRequest request) {
        return !request.isBuy();
    }
    
    @Override
    @ExecutionTimeLog
    public void processOrder(Portfolio portfolio, OrderExecutionRequest request, BigDecimal orderAmount) {
        // 매도 주문 검증
        PortfolioAsset asset = portfolioValidator.validateSellOrder(portfolio, request.getSymbol(), request.getAmount());
        
        // 포트폴리오 잔고 업데이트
        portfolio.processSellOrder(request.getSymbol(), request.getAmount(), request.getPrice(), orderAmount);
        
        // 자산 업데이트
        assetManager.updateAssetOnSell(portfolio, asset, request.getAmount());
    }
}