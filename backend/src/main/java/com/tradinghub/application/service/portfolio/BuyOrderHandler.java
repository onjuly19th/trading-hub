package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tradinghub.application.dto.UpdatePortfolioCommand;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;

import lombok.RequiredArgsConstructor;

/**
 * 매수 주문 처리 전략
 */
@Component
@RequiredArgsConstructor
public class BuyOrderHandler implements PortfolioOrderHandler {
    
    private final PortfolioValidator portfolioValidator;
    private final PortfolioAssetManager assetManager;
    private final PortfolioRepository portfolioRepository;
    
    @Override
    public boolean supports(UpdatePortfolioCommand command) {
        return command.side().equals(OrderSide.BUY);
    }
    
    @Override
    @ExecutionTimeLog
    public void processOrder(Portfolio portfolio, UpdatePortfolioCommand command, BigDecimal orderAmount) {
        // 매수 주문 검증
        portfolioValidator.validateBuyOrder(portfolio, orderAmount);
        
        // 포트폴리오 잔고 업데이트
        portfolio.processBuyOrder(command.symbol(), command.amount(), command.price(), orderAmount);
        
        // 자산 업데이트
        assetManager.updateAssetOnBuy(portfolio, command.symbol(), command.amount(), command.price());
        
        // 포트폴리오 저장
        portfolioRepository.save(portfolio);
    }
}