package com.tradinghub.domain.portfolio;

import com.tradinghub.domain.user.User;
import com.tradinghub.domain.trading.Trade;
import com.tradinghub.domain.trading.TradeRepository;
import com.tradinghub.domain.trading.dto.TradeRequest;
import com.tradinghub.common.exception.InsufficientAssetException;
import com.tradinghub.common.exception.InsufficientBalanceException;
import com.tradinghub.common.exception.PortfolioNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository assetRepository;
    private final TradeRepository tradeRepository;

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

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Trade executeTrade(Long userId, TradeRequest request) {
        log.info("Executing {} trade for user: {}, symbol: {}, amount: {}, price: {}", 
                 request.getType(), userId, request.getSymbol(), request.getAmount(), request.getPrice());

        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));

        BigDecimal tradeAmount = request.getAmount().multiply(request.getPrice());

        // 주문 생성
        Trade trade = createTrade(portfolio, request);
        
        if (request.isBuy()) {
            validateBuyTrade(portfolio, tradeAmount);
            // 매수 처리
            portfolio.processBuyTrade(trade);
            // 자산 업데이트
            updateAssetOnBuyTrade(portfolio, request.getSymbol(), request.getAmount(), request.getPrice());
        } else {
            PortfolioAsset asset = validateSellTrade(portfolio, request.getSymbol(), request.getAmount());
            // 매도 처리
            portfolio.processSellTrade(trade);
            // 자산 업데이트
            updateAssetOnSellTrade(portfolio, asset, request.getAmount(), request.getPrice());
        }

        // 포트폴리오 저장
        portfolioRepository.save(portfolio);
        
        log.info("Trade executed successfully with id: {}", trade.getId());
        return trade;
    }

    @Transactional(readOnly = true)
    public List<Trade> getTradeHistory(Long userId) {
        return tradeRepository.findByPortfolioUserIdOrderByExecutedAtDesc(userId);
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
            //TODO: 웹소켓 비활성화로 인해 주석 처리
            //asset.setCurrentPrice(price);
            //asset.updateProfitLoss();
            assetRepository.save(asset);
        }
    }

    private Trade createTrade(Portfolio portfolio, TradeRequest request) {
        Trade trade = new Trade();
        trade.setPortfolio(portfolio);
        trade.setSymbol(request.getSymbol());
        trade.setType(request.getType());
        trade.setAmount(request.getAmount());
        trade.setPrice(request.getPrice());
        trade.calculateTotal();
        return tradeRepository.save(trade);
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
        //TODO: 웹소켓 비활성화로 인해 주석 처리
        //asset.setCurrentPrice(price);
        //asset.updateProfitLoss();
    }
} 