package com.tradinghub.domain.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tradinghub.application.exception.portfolio.AssetNotFoundException;
import com.tradinghub.application.exception.portfolio.InsufficientAssetException;
import com.tradinghub.application.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioAsset;
import com.tradinghub.domain.model.portfolio.PortfolioAssetRepository;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 관련 검증을 담당하는 클래스
 */
@Component
@RequiredArgsConstructor
public class PortfolioValidator {
    
    private final PortfolioAssetRepository assetRepository;
    
    /**
     * 매수 주문에 대한 포트폴리오 검증을 수행합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param orderAmount 주문 금액
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     */
    public void validateBuyOrder(Portfolio portfolio, BigDecimal orderAmount) {
        if (portfolio.getAvailableBalance().compareTo(orderAmount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %s, Available: %s",
                    orderAmount, portfolio.getAvailableBalance())
            );
        }
    }
    
    /**
     * 매도 주문에 대한 자산 검증을 수행합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param symbol 매도할 자산 심볼
     * @param amount 매도 수량
     * @return 검증된 자산 정보
     * @throws AssetNotFoundException 자산을 찾을 수 없는 경우
     * @throws InsufficientAssetException 매도할 자산이 부족한 경우
     */
    public PortfolioAsset validateSellOrder(Portfolio portfolio, String symbol, BigDecimal amount) {
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseThrow(() -> new AssetNotFoundException("Asset not found: " + symbol));
            
        if (asset.getAmount().compareTo(amount) < 0) {
            throw new InsufficientAssetException(
                String.format("Insufficient asset amount. Required: %s, Available: %s",
                    amount, asset.getAmount())
            );
        }
        return asset;
    }
}
