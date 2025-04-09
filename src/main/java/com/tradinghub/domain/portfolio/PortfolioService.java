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

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository assetRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio createPortfolio(User user, String symbol, BigDecimal initialBalance) {
        log.info("Creating portfolio: userId={}, username={}, symbol={}, initialBalance={}", 
                 user.getId(), user.getUsername(), symbol, initialBalance);

        try {
            Portfolio portfolio = Portfolio.createWithBalance(user, symbol, initialBalance);
            Portfolio savedPortfolio = portfolioRepository.save(portfolio);
            log.info("Portfolio created: userId={}, portfolioId={}, initialBalance={}", 
                    user.getId(), savedPortfolio.getId(), initialBalance);
            return savedPortfolio;
        } catch (Exception e) {
            log.error("Failed to create portfolio: userId={}, username={}, error={}", 
                    user.getId(), user.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        log.debug("Fetching portfolio: userId={}", userId);
        return portfolioRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.warn("Portfolio not found: userId={}", userId);
                return new PortfolioNotFoundException("Portfolio not found for user: " + userId);
            });
    }

    private void validateBuyTrade(Portfolio portfolio, BigDecimal tradeAmount) {
        if (portfolio.getAvailableBalance().compareTo(tradeAmount) < 0) {
            log.warn("Insufficient balance for buy trade: portfolioId={}, required={}, available={}", 
                    portfolio.getId(), tradeAmount, portfolio.getAvailableBalance());
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %s, Available: %s",
                    tradeAmount, portfolio.getAvailableBalance())
            );
        }
    }

    private PortfolioAsset validateSellTrade(Portfolio portfolio, String symbol, BigDecimal amount) {
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseThrow(() -> {
                log.warn("Asset not found for sell trade: portfolioId={}, symbol={}", portfolio.getId(), symbol);
                return new AssetNotFoundException("Asset not found: " + symbol);
            });
            
        if (asset.getAmount().compareTo(amount) < 0) {
            log.warn("Insufficient asset amount for sell trade: portfolioId={}, symbol={}, required={}, available={}", 
                    portfolio.getId(), symbol, amount, asset.getAmount());
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
        log.info("Updating portfolio for order: userId={}, symbol={}, side={}, amount={}, price={}", 
                 userId, request.getSymbol(), request.getSide(), request.getAmount(), request.getPrice());

        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> {
                log.warn("Portfolio not found for update: userId={}", userId);
                return new PortfolioNotFoundException("Portfolio not found for user: " + userId);
            });

        BigDecimal tradeAmount = request.getAmount().multiply(request.getPrice());
        
        try {
            if (request.isBuy()) {
                log.debug("Processing buy trade: userId={}, symbol={}, amount={}, price={}", 
                        userId, request.getSymbol(), request.getAmount(), request.getPrice());
                validateBuyTrade(portfolio, tradeAmount);
                // processBuyTrade는 내부적으로 usdBalance를 업데이트함
                portfolio.processBuyTrade(request.getSymbol(), request.getAmount(), request.getPrice(), tradeAmount);
                // 자산 업데이트
                updateAssetOnBuyTrade(portfolio, request.getSymbol(), request.getAmount(), request.getPrice());
            } else {
                log.debug("Processing sell trade: userId={}, symbol={}, amount={}, price={}", 
                        userId, request.getSymbol(), request.getAmount(), request.getPrice());
                PortfolioAsset asset = validateSellTrade(portfolio, request.getSymbol(), request.getAmount());
                // processSellTrade는 내부적으로 usdBalance를 업데이트함
                portfolio.processSellTrade(request.getSymbol(), request.getAmount(), request.getPrice(), tradeAmount);
                // 자산 업데이트
                updateAssetOnSellTrade(portfolio, asset, request.getAmount(), request.getPrice());
            }

            // 포트폴리오 저장
            portfolioRepository.save(portfolio);
            
            log.info("Portfolio updated successfully: userId={}, symbol={}, side={}", 
                    userId, request.getSymbol(), request.getSide());
        } catch (Exception e) {
            log.error("Failed to update portfolio: userId={}, symbol={}, error={}", 
                    userId, request.getSymbol(), e.getMessage(), e);
            throw e;
        }
    }
} 