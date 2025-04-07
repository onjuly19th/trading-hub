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
import com.tradinghub.common.exception.InsufficientAssetException;
import com.tradinghub.common.exception.InsufficientBalanceException;
import com.tradinghub.common.exception.PortfolioNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository assetRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio createPortfolio(User user, String symbol, BigDecimal initialBalance) {
        log.info("Creating portfolio for user: {}, symbol: {}, initialBalance: {}", 
                 user.getUsername(), symbol, initialBalance);

        try {
            Portfolio portfolio = Portfolio.createWithBalance(user, symbol, initialBalance);
            Portfolio savedPortfolio = portfolioRepository.save(portfolio);
            log.info("Portfolio saved successfully with id: {}", savedPortfolio.getId());
            user.setPortfolio(savedPortfolio);
            return savedPortfolio;
        } catch (Exception e) {
            log.error("Error creating portfolio", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        return portfolioRepository.findByUserId(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));
    }

    private void validateBuyTrade(Portfolio portfolio, BigDecimal tradeAmount) {
        if (portfolio.getAvailableBalance().compareTo(tradeAmount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %s, Available: %s",
                    tradeAmount, portfolio.getAvailableBalance())
            );
        }
    }

    private PortfolioAsset validateSellTrade(Portfolio portfolio, String symbol, BigDecimal amount) {
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseThrow(() -> new RuntimeException("Asset not found: " + symbol));
            
        if (asset.getAmount().compareTo(amount) < 0) {
            throw new InsufficientAssetException(
                String.format("Insufficient asset amount. Required: %s, Available: %s",
                    amount, asset.getAmount())
            );
        }
        return asset;
    }

    private void updateAssetOnBuyTrade(Portfolio portfolio, String symbol, BigDecimal amount, BigDecimal price) {
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

    private void updateAssetOnSellTrade(Portfolio portfolio, PortfolioAsset asset, BigDecimal amount, BigDecimal price) {
        asset.setAmount(asset.getAmount().subtract(amount));
        if (asset.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            assetRepository.delete(asset);
        } else {
            assetRepository.save(asset);
        }
    }

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
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updatePortfolioForOrder(Long userId, OrderExecutionRequest request) {
        log.info("Updating portfolio for user: {}, symbol: {}, amount: {}, price: {}, side: {}", 
                 userId, request.getSymbol(), request.getAmount(), request.getPrice(), request.getSide());

        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));

        BigDecimal tradeAmount = request.getAmount().multiply(request.getPrice());
        
        if (request.isBuy()) {
            validateBuyTrade(portfolio, tradeAmount);
            // processBuyTrade는 내부적으로 usdBalance를 업데이트함
            portfolio.processBuyTrade(request.getSymbol(), request.getAmount(), request.getPrice(), tradeAmount);
            // 자산 업데이트
            updateAssetOnBuyTrade(portfolio, request.getSymbol(), request.getAmount(), request.getPrice());
        } else {
            PortfolioAsset asset = validateSellTrade(portfolio, request.getSymbol(), request.getAmount());
            // processSellTrade는 내부적으로 usdBalance를 업데이트함
            portfolio.processSellTrade(request.getSymbol(), request.getAmount(), request.getPrice(), tradeAmount);
            // 자산 업데이트
            updateAssetOnSellTrade(portfolio, asset, request.getAmount(), request.getPrice());
        }

        // 포트폴리오 저장
        portfolioRepository.save(portfolio);
        
        log.info("Portfolio updated successfully for user: {}", userId);
    }
} 