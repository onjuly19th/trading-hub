package com.tradinghub.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.domain.user.User;
import com.tradinghub.domain.trading.dto.OrderExecutionRequest;
import com.tradinghub.common.exception.AssetNotFoundException;
import com.tradinghub.common.exception.InsufficientAssetException;
import com.tradinghub.common.exception.InsufficientBalanceException;
import com.tradinghub.common.exception.PortfolioNotFoundException;
import com.tradinghub.infrastructure.aop.LogExecutionTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository assetRepository;

    @LogExecutionTime
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio createPortfolio(User user, String symbol, BigDecimal initialBalance) {
        try {
            Portfolio portfolio = Portfolio.createWithBalance(user, symbol, initialBalance);
            return portfolioRepository.save(portfolio);
        } catch (Exception e) {
            throw e;
        }
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        return portfolioRepository.findByUserIdWithAssets(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));
    }

    @LogExecutionTime
    private void validateBuyOrder(Portfolio portfolio, BigDecimal orderAmount) {
        if (portfolio.getAvailableBalance().compareTo(orderAmount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %s, Available: %s",
                    orderAmount, portfolio.getAvailableBalance())
            );
        }
    }

    @LogExecutionTime
    private PortfolioAsset validateSellOrder(Portfolio portfolio, String symbol, BigDecimal amount) {
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

    @LogExecutionTime
    private void updateAssetOnBuyOrder(Portfolio portfolio, String symbol, BigDecimal amount, BigDecimal price) {
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseGet(() -> {
                PortfolioAsset newAsset = new PortfolioAsset();
                newAsset.setPortfolio(portfolio);
                newAsset.setSymbol(symbol);
                return newAsset;
            });

        updateAssetOnBuy(asset, amount, price);
        assetRepository.save(asset);
    }

    @LogExecutionTime
    private void updateAssetOnBuy(PortfolioAsset asset, BigDecimal amount, BigDecimal price) {
        BigDecimal oldAmount = asset.getAmount() != null ? asset.getAmount() : BigDecimal.ZERO;
        BigDecimal oldAveragePrice = asset.getAveragePrice() != null ? asset.getAveragePrice() : BigDecimal.ZERO;
        BigDecimal newAmount = oldAmount.add(amount);
        
        // 평균 매수가 계산: ((기존수량 * 기존평균가) + (신규수량 * 현재가)) / 총수량
        BigDecimal totalCost = oldAmount.multiply(oldAveragePrice).add(amount.multiply(price));
        BigDecimal newAveragePrice = totalCost.divide(newAmount, 4, RoundingMode.HALF_UP);
        
        asset.setAmount(newAmount);
        asset.setAveragePrice(newAveragePrice);
    }

    /**
     * 주문 정보로 포트폴리오 업데이트
     * 주문 체결 시 포트폴리오 잔액과 자산을 업데이트함
     * 
     * @param userId 사용자 ID
     * @param request 주문 실행 요청 정보
     */
    @LogExecutionTime
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updatePortfolioForOrder(Long userId, OrderExecutionRequest request) {
        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));

        BigDecimal orderAmount = request.getAmount().multiply(request.getPrice());

        try {
            if (request.isBuy()) {
                validateBuyOrder(portfolio, orderAmount);
                // processBuyOrder는 내부적으로 usdBalance를 업데이트함
                portfolio.processBuyOrder(request.getSymbol(), request.getAmount(), request.getPrice(), orderAmount);
                // 자산 업데이트
                updateAssetOnBuyOrder(portfolio, request.getSymbol(), request.getAmount(), request.getPrice());
                
                // 포트폴리오 저장
                portfolioRepository.save(portfolio);
            } else {
                PortfolioAsset asset = validateSellOrder(portfolio, request.getSymbol(), request.getAmount());
                // processSellOrder는 내부적으로 usdBalance를 업데이트함
                portfolio.processSellOrder(request.getSymbol(), request.getAmount(), request.getPrice(), orderAmount);
                
                asset.setAmount(asset.getAmount().subtract(request.getAmount()));
                
                if (asset.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                    // 1. 먼저 포트폴리오의 자산 컬렉션에서 제거
                    if (portfolio.getAssets() != null) {
                        portfolio.getAssets().remove(asset);
                    }
                    
                    // 2. 포트폴리오를 먼저 저장 (자산이 제거된 상태로)
                    portfolioRepository.save(portfolio);
                    
                    // 3. 그 다음 자산 엔티티 삭제
                    assetRepository.delete(asset);
                    
                    log.info("Asset deleted for portfolio: portfolioId={}, symbol={}", 
                             portfolio.getId(), asset.getSymbol());
                } else {
                    // 자산 업데이트 후 저장
                    assetRepository.save(asset);
                    portfolioRepository.save(portfolio);
                }
            }
        } catch (Exception e) {
            log.error("Error updating portfolio: {}", e.getMessage(), e);
            throw e;
        }
    }

    @LogExecutionTime
    private void updateAssetOnSellOrder(Portfolio portfolio, PortfolioAsset asset, BigDecimal amount, BigDecimal price) {
        // 이 메서드를 더 이상 사용하지 않음 - 로직을 updatePortfolioForOrder로 옮김
    }
} 