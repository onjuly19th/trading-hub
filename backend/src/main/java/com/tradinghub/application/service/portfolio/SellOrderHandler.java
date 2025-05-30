package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tradinghub.application.dto.UpdatePortfolioCommand;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioAsset;
import com.tradinghub.domain.service.PortfolioAssetManager;
import com.tradinghub.domain.service.PortfolioValidator;

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
    public boolean supports(UpdatePortfolioCommand command) {
        return !command.side().equals(OrderSide.BUY);
    }
    
    @Override
    public void processOrder(Portfolio portfolio, UpdatePortfolioCommand command, BigDecimal orderAmount) {
        // 매도 주문 검증
        PortfolioAsset asset = portfolioValidator.validateSellOrder(portfolio, command.symbol(), command.amount());
        
        // 포트폴리오 잔고 업데이트
        portfolio.processSellOrder(command.symbol(), command.amount(), command.price(), orderAmount);
        
        // 자산 업데이트
        assetManager.updateAssetOnSell(portfolio, asset, command.amount());
    }
}